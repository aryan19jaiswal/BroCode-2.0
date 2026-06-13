// NOT USED ANYWHERE AS OF NOW, BUT CAN BE USED IN THE FUTURE TO ADD MORE DOCUMENTS TO THE EMBEDDING STORE AND RETRIEVE THEM BASED ON SIMILARITY

package com.broCode.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class GeminiEmbeddingService {

    @Value("${gemini.api.key}")
    private String geminiAPIKey;

    private static final double SIMILARITY_THRESHOLD = 0.7;
    private static final int MAX_RESULTS = 25;

    private GoogleAiEmbeddingModel embeddingModel;
    private EmbeddingStoreIngestor embeddingStoreIngestor;
    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

    @PostConstruct
    public void init() {
        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(geminiAPIKey)
                .modelName("text-embedding-004")
                .build();

        this.embeddingStoreIngestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        log.info("GeminiEmbeddingService initialized");
    }

    public void ingest(List<Document> documents) {
        embeddingStoreIngestor.ingest(documents);
    }

    public Document toDocument(String text, Map<String, String> metadata) {
        Document document = Document.from(text);
        metadata.forEach((k, v) -> document.metadata().put(k, v));
        return document;
    }

    public void ingest(String text, Map<String, String> metadata) {
        ingest(List.of(toDocument(text, metadata)));
    }

    public void ingest(String text) { ingest(text, Map.of()); }

    public ContentRetriever getRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .minScore(SIMILARITY_THRESHOLD)
                .maxResults(MAX_RESULTS)
                .build();
    }

    public ContentRetriever getRetriever(Filter filter) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .minScore(SIMILARITY_THRESHOLD)
                .maxResults(MAX_RESULTS)
                .filter(filter)
                .build();
    }

    public ContentRetriever getRetriever(Function<Query, Filter> dynamicFilter) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .minScore(SIMILARITY_THRESHOLD)
                .maxResults(MAX_RESULTS)
                .dynamicFilter(dynamicFilter)
                .build();
    }

    public void removeAll(List<String> ids) { embeddingStore.removeAll(ids); }
    public void remove(String id) { embeddingStore.remove(id); }
    public void removeAll(Filter filter) { embeddingStore.removeAll(filter); }
}
