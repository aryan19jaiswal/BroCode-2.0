package com.broCode.service;

import com.broCode.exception.InvalidSessionException;
import com.broCode.model.ChatSession;
import com.broCode.model.ChatSessionContext;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory chat session store with automatic TTL-based expiration.
 * Sessions that have not been accessed within the configured TTL are evicted by
 * a scheduled cleanup task to prevent memory leaks.
 */
@Slf4j
@Component
public class InMemoryChatSessionService implements ChatSessionService {

    private final ConcurrentHashMap<String, ChatSession> chatSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastAccessMap = new ConcurrentHashMap<>();

    @Value("${chat.session.ttl-minutes:30}")
    private int sessionTtlMinutes;

    @Override
    public String startNewChatSession(ChatSessionContext context, String systemPrompt) {
        String sessionId = UUID.randomUUID().toString();
        chatSessionMap.put(sessionId, new ChatSession(context, List.of(SystemMessage.from(systemPrompt))));
        touchSession(sessionId);
        log.debug("Created new chat session: {}", sessionId);
        return sessionId;
    }

    @Override
    public List<ChatMessage> getMessages(Object sessionId) {
        validateSessionId(sessionId.toString());
        touchSession(sessionId.toString());
        return chatSessionMap.get(sessionId.toString()).getChatHistory();
    }

    @Override
    public void addChatMessage(String sessionId, ChatMessage message) {
        validateSessionId(sessionId);
        touchSession(sessionId);
        chatSessionMap.computeIfPresent(sessionId, (key, chatSession) -> {
            List<ChatMessage> messages = new ArrayList<>(chatSession.getChatHistory());
            messages.add(message);
            chatSession.setChatHistory(messages);
            return chatSession;
        });
    }

    @Override
    public void validateSessionId(String sessionId) {
        if (!this.chatSessionMap.containsKey(sessionId)) {
            throw new InvalidSessionException(sessionId);
        }
    }

    @Override
    public void updateMessages(Object sessionId, List<ChatMessage> chatMessages) {
        validateSessionId(sessionId.toString());
        touchSession(sessionId.toString());
        chatSessionMap.computeIfPresent(sessionId.toString(), (key, chatSession) -> {
            chatSession.setChatHistory(chatMessages);
            return chatSession;
        });
    }

    @Override
    public void deleteMessages(Object sessionId) {
        validateSessionId(sessionId.toString());
        chatSessionMap.computeIfPresent(sessionId.toString(), (key, chatSession) -> {
            chatSession.getChatHistory().clear();
            return chatSession;
        });
    }

    @Override
    public ChatSessionContext getChatSessionContext(String sessionId) {
        validateSessionId(sessionId);
        touchSession(sessionId);
        return chatSessionMap.get(sessionId).getContext();
    }

    @Override
    public void destroyChatSession(String sessionId) {
        chatSessionMap.remove(sessionId);
        lastAccessMap.remove(sessionId);
        log.debug("Destroyed chat session: {}", sessionId);
    }

    /**
     * Scheduled TTL cleanup — runs every 5 minutes.
     * Evicts sessions that have not been accessed within sessionTtlMinutes.
     */
    @Scheduled(fixedRateString = "300000", initialDelayString = "300000")
    public void evictExpiredSessions() {
        Instant cutoff = Instant.now().minusSeconds((long) sessionTtlMinutes * 60);
        int evicted = 0;

        Iterator<Map.Entry<String, Instant>> it = lastAccessMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Instant> entry = it.next();
            if (entry.getValue().isBefore(cutoff)) {
                chatSessionMap.remove(entry.getKey());
                it.remove();
                evicted++;
            }
        }

        if (evicted > 0) {
            log.info("Evicted {} expired chat sessions. Active sessions: {}", evicted, chatSessionMap.size());
        }
    }

    private void touchSession(String sessionId) {
        lastAccessMap.put(sessionId, Instant.now());
    }
}

