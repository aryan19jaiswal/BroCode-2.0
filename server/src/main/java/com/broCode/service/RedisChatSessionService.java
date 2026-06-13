package com.broCode.service;

import com.broCode.dto.StoredMessage;
import com.broCode.exception.InvalidSessionException;
import com.broCode.model.ChatSessionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Redis-backed chat session store for staging and production profiles.
 *
 * Each session is a single Redis key holding a JSON array of StoredMessage objects.
 * TTL is reset on every read or write, matching the idle-timeout semantics of
 * InMemoryChatSessionService. Sessions that expire are naturally evicted by Redis
 * — no @Scheduled cleanup job needed.
 *
 * Serialization uses a simple role+content DTO (StoredMessage) rather than
 * LangChain4j's internal classes, so this is safe across LangChain4j upgrades.
 */
@Slf4j
@Profile("!local")
@Component
public class RedisChatSessionService implements ChatSessionService {

    private static final String KEY_PREFIX = "chat:session:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int sessionTtlMinutes;

    public RedisChatSessionService(StringRedisTemplate redis,
                                   ObjectMapper objectMapper,
                                   @Value("${chat.session.ttl-minutes:30}") int sessionTtlMinutes) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    // ── ChatSessionService ──────────────────────────────────────────────────

    @Override
    public String startNewChatSession(String systemPrompt) {
        String sessionId = UUID.randomUUID().toString();
        persist(sessionId, List.of(SystemMessage.from(systemPrompt)));
        log.debug("Created Redis chat session: {}", sessionId);
        return sessionId;
    }

    @Override
    public void addChatMessage(String sessionId, ChatMessage message) {
        List<ChatMessage> updated = new ArrayList<>(load(sessionId));
        updated.add(message);
        persist(sessionId, updated);
    }

    @Override
    public void validateSessionId(String sessionId) {
        if (!Boolean.TRUE.equals(redis.hasKey(key(sessionId)))) {
            throw new InvalidSessionException(sessionId);
        }
    }

    @Override
    public ChatSessionContext getChatSessionContext(String sessionId) {
        return null; // Not used in current implementation
    }

    @Override
    public void restoreSession(String sessionId, List<ChatMessage> messages) {
        persist(sessionId, messages);
        log.debug("Restored session {} into Redis ({} messages)", sessionId, messages.size());
    }

    @Override
    public void destroyChatSession(String sessionId) {
        redis.delete(key(sessionId));
        log.debug("Destroyed Redis chat session: {}", sessionId);
    }

    // ── ChatMemoryStore (called by LangChain4j's TokenWindowChatMemory) ────

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        validateSessionId(sessionId);
        touch(sessionId);
        return load(sessionId);
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        persist(memoryId.toString(), messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redis.delete(key(memoryId.toString()));
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private void persist(String sessionId, List<ChatMessage> messages) {
        List<StoredMessage> stored = messages.stream().map(this::toStored).toList();
        try {
            redis.opsForValue().set(
                    key(sessionId),
                    objectMapper.writeValueAsString(stored),
                    Duration.ofMinutes(sessionTtlMinutes)
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chat session: " + sessionId, e);
        }
    }

    private List<ChatMessage> load(String sessionId) {
        String json = redis.opsForValue().get(key(sessionId));
        if (json == null || json.isBlank()) return List.of();
        try {
            List<StoredMessage> stored = objectMapper.readValue(json, new TypeReference<>() {});
            return stored.stream().map(this::fromStored).toList();
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat session: {} — returning empty history", sessionId, e);
            return List.of();
        }
    }

    private void touch(String sessionId) {
        redis.expire(key(sessionId), Duration.ofMinutes(sessionTtlMinutes));
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }

    private StoredMessage toStored(ChatMessage msg) {
        String role = getChatMessageRole(msg);
        String content = switch (msg.type()) {
            case SYSTEM -> ((SystemMessage) msg).text();
            case USER   -> ((UserMessage)   msg).singleText();
            case AI     -> {
                String text = ((AiMessage) msg).text();
                yield text != null ? text : "";
            }
            default -> "";
        };
        return new StoredMessage(role, content);
    }

    private ChatMessage fromStored(StoredMessage stored) {
        return switch (stored.role()) {
            case "system"    -> SystemMessage.from(stored.content());
            case "user"      -> UserMessage.from(stored.content());
            case "assistant" -> AiMessage.from(stored.content());
            default -> throw new IllegalArgumentException("Unknown message role: " + stored.role());
        };
    }
}
