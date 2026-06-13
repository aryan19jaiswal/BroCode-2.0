package com.broCode.model;

import dev.langchain4j.data.message.ChatMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChatSession {
    ChatSessionContext context;

    @Getter(AccessLevel.NONE)
    List<ChatMessage> chatHistory;

    public List<ChatMessage> getChatHistory() {
        return Collections.unmodifiableList(chatHistory);
    }
}
