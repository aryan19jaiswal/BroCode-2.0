package com.broCode.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

public interface Assistant {

    Flux<String> chatStream(@MemoryId String sessionId, @UserMessage String question);
    TokenStream chatTokenStream(@MemoryId String sessionId, @UserMessage String question);
}
