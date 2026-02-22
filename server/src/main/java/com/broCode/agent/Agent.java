package com.broCode.agent;

import com.broCode.service.LLMService;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public class Agent {

    private final Tools tools; // Tools that the agent can use, can be null if no tools are needed
    private final LLMService llmService;

    public Agent(LLMService llmService) {
        this.tools = null; // Default implementation sets tool to null, can be overridden by subclasses
        this.llmService = llmService;
    }

    protected ContentRetriever getContentRetriever() {
        return null; // Default implementation returns null, can be overridden by subclasses
    }

    public Flux<String> chatStream(String sessionId, String question) {
        ContentRetriever contentRetriever = getContentRetriever();

        if(tools!=null && contentRetriever!=null){
            return llmService.chatStream(contentRetriever, tools, sessionId, question);
        }
        else if(tools!=null){
            return llmService.chatStream(tools, sessionId, question);
        }
        else if(contentRetriever!=null){
            return llmService.chatStream(contentRetriever, sessionId, question);
        }
        return llmService.chatStream(sessionId, question);
    }
}
