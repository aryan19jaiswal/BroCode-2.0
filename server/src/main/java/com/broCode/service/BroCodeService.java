package com.broCode.service;

import com.broCode.agent.BroCodeAgent;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Component
public class BroCodeService {

    private final ChatSessionService chatSessionService;
    private final BroCodeAgent broCodeAgent;
    private final ChatPersistenceService chatPersistenceService;

    public BroCodeService(ChatSessionService chatSessionService,
                          BroCodeAgent broCodeAgent,
                          ChatPersistenceService chatPersistenceService) {
        this.chatSessionService = chatSessionService;
        this.broCodeAgent = broCodeAgent;
        this.chatPersistenceService = chatPersistenceService;
    }

    /**
     * Creates a new session in both the cache tier and MongoDB.
     * The title is derived from the first user message (truncated).
     */
    public String createNewChatSession(String userId, String title) {
        String sessionId = chatSessionService.startNewChatSession(PromptService.BRO_CODE_SYSTEM_PROMPT);
        chatPersistenceService.createSession(sessionId, userId, title);
        return sessionId;
    }

    /**
     * Returns the sessionId if it exists in the live cache. If the cache TTL
     * has expired, transparently restores the session from MongoDB and returns
     * the same sessionId so the conversation continues seamlessly.
     * Throws InvalidSessionException if the session is not found anywhere.
     */
    public String resolveSession(String sessionId, String userId) {
        try {
            chatSessionService.validateSessionId(sessionId);
            return sessionId;
        } catch (Exception e) {
            chatPersistenceService.restoreToCache(sessionId, userId, chatSessionService);
            return sessionId;
        }
    }

    public Flux<String> getBroCodeAgentResponse(String sessionId, String question) {
        return broCodeAgent.chatStream(sessionId, question);
    }

    /**
     * Best-effort MongoDB sync. Called fire-and-forget after each stream completes.
     * By the time this is invoked, LangChain4j has already written both the user
     * message and the full AI response to the cache.
     */
    public void syncSessionToMongo(String sessionId) {
        try {
            List<ChatMessage> messages = chatSessionService.getMessages(sessionId);
            chatPersistenceService.syncMessages(sessionId, messages);
        } catch (Exception e) {
            log.warn("Failed to sync session {} to MongoDB: {}", sessionId, e.getMessage());
        }
    }
}
