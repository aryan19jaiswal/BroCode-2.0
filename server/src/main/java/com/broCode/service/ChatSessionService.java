package com.broCode.service;

import com.broCode.model.ChatSessionContext;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public interface ChatSessionService extends ChatMemoryStore {

    String startNewChatSession(ChatSessionContext context, String systemPrompt);

    void addChatMessage(String sessionId, ChatMessage chatMessage);

    void validateSessionId(String sessionId);

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
