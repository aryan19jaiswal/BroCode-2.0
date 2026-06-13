package com.broCode.service;

import com.broCode.model.ChatSessionContext;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.List;



public interface ChatSessionService extends ChatMemoryStore {

    String startNewChatSession(String systemPrompt);

    void addChatMessage(String sessionId, ChatMessage chatMessage);

    void validateSessionId(String sessionId);

    /**
     * Writes a pre-built message list directly into the cache, bypassing
     * validateSessionId. Used to restore sessions whose TTL has expired by
     * re-populating them from the MongoDB copy.
     */
    void restoreSession(String sessionId, List<ChatMessage> messages);

    default String getChatMessageRole(ChatMessage chatMessage){
        return switch (chatMessage.type()){
            case USER -> "user";
            case AI -> "assistant";
            case SYSTEM -> "system";
            default -> throw new IllegalArgumentException("Invalid Message Type: " + chatMessage.type());
        };
    }

    ChatSessionContext getChatSessionContext(String sessionId);

    void destroyChatSession(String sessionId);

}
