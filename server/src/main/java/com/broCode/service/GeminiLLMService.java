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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
public class GeminiLLMService implements LLMService {

    private final GoogleAiGeminiStreamingChatModel geminiStreamingChatModel;
    private final GoogleAiGeminiTokenCountEstimator tokenizer;
    private final ChatMemoryProvider chatMemoryProvider;

    // Global Gemini API quota guards — blocking by nature, but offloaded to boundedElastic (see chatStream).
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

        this.tokenizer = GoogleAiGeminiTokenCountEstimator.builder()
                .apiKey(geminiAPIKey)
                .modelName(geminiModelName)
                .build();

        this.geminiStreamingChatModel = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiAPIKey)
                .modelName(geminiModelName)
                .maxOutputTokens(maxOutputTokens)
                .temperature(geminiTemperature)
                .topP(geminiTopP)
                .allowGoogleSearch(true)
                .build();

        this.chatMemoryProvider = sessionId -> TokenWindowChatMemory.builder()
                .id(sessionId)
                .maxTokens(128000, tokenizer)
                .chatMemoryStore(chatSessionService)
                .build();

        this.requestRateLimiter = RateLimiter.create((double) rpm / 60);
        this.tokenRateLimiter = RateLimiter.create((double) tpm / 60);

        // Cache stateless AiServices instances — building them per-request was wasteful.
        // State (conversation history) lives in the ChatMemoryStore, not in the AiServices object.
        this.assistant = AiServices.builder(Assistant.class)
                .chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(geminiStreamingChatModel)
                .build();
    }

    /**
     * Offloads the blocking RateLimiter.acquire() calls to boundedElastic so they
     * never pin a Reactor scheduler thread while waiting for the token bucket to refill.
     * The Gemini streaming itself then runs on LangChain4j's own thread pool.
     */
    @Override
    public Flux<String> chatStream(String sessionId, String question) {
        return Mono.fromRunnable(() -> acquireLimits(sessionId, question))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(ignored -> log.info("Streaming response for session: {}", sessionId))
                .thenMany(Flux.defer(() -> assistant.chatStream(sessionId, question)));
    }

    @Override
    public TokenStream chatTokenStream(String sessionId, String question) {
        acquireLimits(sessionId, question);
        return assistant.chatTokenStream(sessionId, question);
    }

    @Override
    public TokenStream chatTokenStream(ContentRetriever contentRetriever, String sessionId, String question) {
        acquireLimits(sessionId, question);
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
        return AiServices.builder(Assistant.class)
                .chatMemoryProvider(chatMemoryProvider)
                .streamingChatModel(geminiStreamingChatModel)
                .contentRetriever(retriever)
                .tools(tools)
                .build()
                .chatTokenStream(sessionId, question);
    }

    private void acquireLimits(String sessionId, String question) {
        requestRateLimiter.acquire(1);
        int estimatedTokens = tokenizer.estimateTokenCountInMessages(
                chatMemoryProvider.get(sessionId).messages())
                + tokenizer.estimateTokenCountInText(question);
        tokenRateLimiter.acquire(estimatedTokens);
    }

    private void acquireLimits(List<ChatMessage> messages) {
        requestRateLimiter.acquire(1);
        tokenRateLimiter.acquire(tokenizer.estimateTokenCountInMessages(messages));
    }
}
