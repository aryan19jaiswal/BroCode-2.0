package com.broCode.model;

import dev.langchain4j.data.message.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ChatSession {
    ChatSessionContext context;
    List<ChatMessage> chatHistory;
}
