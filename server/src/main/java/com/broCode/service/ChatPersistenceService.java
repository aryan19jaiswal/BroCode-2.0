package com.broCode.service;

import com.broCode.dto.ChatSessionResponse;
import com.broCode.dto.StoredMessage;
import com.broCode.exception.InvalidSessionException;
import com.broCode.model.ChatSessionDocument;
import com.broCode.repository.ChatSessionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the durable MongoDB copy of each chat session.
 *
 * The live copy lives in the cache tier (Redis or InMemory). This service is
 * responsible only for the persistent layer: creating sessions, syncing messages
 * after each completed turn, serving history on page load, and cleaning up on
 * explicit delete. Cache restoration on TTL miss is also handled here.
 */
@Slf4j
@Service
public class ChatPersistenceService {

    private final ChatSessionRepository chatSessionRepository;

    public ChatPersistenceService(ChatSessionRepository chatSessionRepository) {
        this.chatSessionRepository = chatSessionRepository;
    }

    public void createSession(String sessionId, String userId, String title) {
        ChatSessionDocument doc = new ChatSessionDocument();
        doc.setId(sessionId);
        doc.setUserId(userId);
        doc.setTitle(title);
        doc.setMessages(List.of());
        chatSessionRepository.save(doc);
        log.debug("Created MongoDB chat session {} for user {}", sessionId, userId);
    }

    public List<ChatSessionResponse> getUserSessions(String userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(doc -> new ChatSessionResponse(
                        doc.getId(),
                        doc.getTitle(),
                        doc.getMessages(),
                        doc.getCreatedAt() != null ? doc.getCreatedAt().toEpochMilli() : 0L
                ))
                .toList();
    }

    /**
     * Persists the current message list for a session. Called fire-and-forget
     * after each streaming turn completes. System messages are excluded — the
     * system prompt is re-injected at cache restore time and must not appear
     * in the user-facing history.
     */
    public void syncMessages(String sessionId, List<ChatMessage> messages) {
        chatSessionRepository.findById(sessionId).ifPresent(doc -> {
            List<StoredMessage> stored = messages.stream()
                    .map(this::toStored)
                    .filter(s -> !"system".equals(s.role()))
                    .toList();
            doc.setMessages(stored);
            chatSessionRepository.save(doc);
            log.debug("Synced {} messages to MongoDB for session {}", stored.size(), sessionId);
        });
    }

    /**
     * Idempotent delete. No-op if the session doesn't exist or doesn't belong
     * to the user (prevents information leakage about other users' sessions).
     */
    public void deleteSession(String sessionId, String userId) {
        chatSessionRepository.deleteByIdAndUserId(sessionId, userId);
        log.debug("Deleted MongoDB chat session {} for user {}", sessionId, userId);
    }

    /**
     * Restores an expired-from-cache session into the live cache tier from MongoDB.
     * Re-injects the system prompt at position 0 so LangChain4j memory
     * management works correctly.
     */
    public void restoreToCache(String sessionId, String userId, ChatSessionService chatSessionService) {
        ChatSessionDocument doc = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new InvalidSessionException(sessionId));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(PromptService.BRO_CODE_SYSTEM_PROMPT));
        messages.addAll(doc.getMessages().stream().map(this::fromStored).toList());

        chatSessionService.restoreSession(sessionId, messages);
        log.debug("Restored session {} from MongoDB into cache ({} messages)", sessionId, messages.size());
    }

    // ── Conversion helpers ─────────────────────────────────────────────────

    private StoredMessage toStored(ChatMessage msg) {
        String role = switch (msg.type()) {
            case USER   -> "user";
            case AI     -> "assistant";
            case SYSTEM -> "system";
            default     -> "unknown";
        };
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
