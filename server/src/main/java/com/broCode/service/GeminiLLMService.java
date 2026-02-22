package com.broCode.service;

import com.broCode.agent.Tools;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class GeminiLLMService implements LLMService {

    private final GoogleAiGeminiStreamingChatModel geminiStreamingChatModel;
    private final GoogleAiGeminiTokenCountEstimator tokenizer;
    private final ChatMemoryProvider chatMemoryProvider;
    private final RateLimiter requestRateLimiter;
    private final RateLimiter tokenRateLimiter;

    private final Assistant assistant;

    public GeminiLLMService(
            ChatSessionService chatSessionService,
            @Value("${gemini.rpm.limit}") int rpm,
            @Value("${gemini.tpm.limit}") int tpm,
            @Value("${gemini.api.key}") String geminiAPIKey,
            @Value("${gemini.model.name}") String geminiModelName,
            @Value("${gemini.temperature}") double geminiTemperature,
            @Value("${gemini.top_p}") double geminiTopP,
            @Value("${gemini.max.output_tokens}") int maxOutputTokens
    ) {
        log.info("Initializing GeminiLLMService with model: {}", geminiModelName);

        // 1. Initialize Tokenizer
        this.tokenizer = GoogleAiGeminiTokenCountEstimator.builder()
                .apiKey(geminiAPIKey)
                .modelName(geminiModelName)
                .build();

        // 2. Initialize Chat Model
        this.geminiStreamingChatModel = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiAPIKey)
                .modelName(geminiModelName)
                .maxOutputTokens(maxOutputTokens)
                .temperature(geminiTemperature)
                .topP(geminiTopP)
                .allowGoogleSearch(true)
                .build();

        // 3. Initialize Memory Provider
        this.chatMemoryProvider = sessionId -> TokenWindowChatMemory.builder()
                .id(sessionId)
                .maxTokens(128000, tokenizer)
                .chatMemoryStore(chatSessionService)
                .build();

        // 4. Initialize Rate Limiters
        this.requestRateLimiter = RateLimiter.create((double) rpm / 60);
        this.tokenRateLimiter = RateLimiter.create((double) tpm / 60);

        // 5. Initialize default Assistant
        this.assistant = AiServices.builder(Assistant.class)
                .chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(geminiStreamingChatModel)
                .build();
    }

    private void acquireLimits(List<ChatMessage> messages){
        requestRateLimiter.acquire(1);
        tokenRateLimiter.acquire(tokenizer.estimateTokenCountInMessages(messages));
    }

    private void acquireLimits(String sessionId, String question){
        requestRateLimiter.acquire(1);
        tokenRateLimiter.acquire(tokenizer.estimateTokenCountInMessages(
                chatMemoryProvider.get(sessionId).messages())+
                tokenizer.estimateTokenCountInText(question));
    }

    /**
     * Streams delta tokens (individual chunks) — NOT accumulated text.
     * Each emission is a single token/chunk from the LLM, which the frontend
     * appends to build the full response. This is bandwidth-efficient and
     * matches the industry standard (OpenAI, Anthropic, etc.).
     */
    public Flux<String> chatStream(String sessionId, String question){
        acquireLimits(sessionId, question);
        log.info("Streaming response for session: {}", sessionId);
        return this.assistant.chatStream(sessionId, question);
    }

    @Override
    public TokenStream chatTokenStream(String sessionId, String question) {
        acquireLimits(sessionId, question);
        log.info("Calling BroCode with Memory");
        return this.assistant.chatTokenStream(sessionId, question);
    }

    @Override
    public TokenStream chatTokenStream(ContentRetriever contentRetriever, String sessionId, String question) {
        acquireLimits(sessionId, question);
        log.info("Calling BroCode with Retriever and Memory");
        return AiServices.builder(Assistant.class)
                .chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(geminiStreamingChatModel)
                .contentRetriever(contentRetriever)
                .build()
                .chatTokenStream(sessionId, question);
    }

    @Override
    public TokenStream chatTokenStream(Tools tools, String sessionId, String question) {
        acquireLimits(sessionId, question);
        log.info("Calling BroCode with Tool and Memory");
        return AiServices.builder(Assistant.class)
                .chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(geminiStreamingChatModel)
                .tools(tools)
                .build()
                .chatTokenStream(sessionId, question);
    }

    @Override
    public TokenStream chatTokenStream(ContentRetriever retriever, Tools tools, String sessionId, String question) {
        acquireLimits(sessionId, question);
        log.info("Calling BroCode with Retriever, Tool and Memory");
        return AiServices.builder(Assistant.class)
                .chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(geminiStreamingChatModel)
                .contentRetriever(retriever)
                .tools(tools)
                .build()
                .chatTokenStream(sessionId, question);
    }
}



