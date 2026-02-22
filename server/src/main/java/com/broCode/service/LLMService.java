package com.broCode.service;

import com.broCode.agent.Tools;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicBoolean;

public interface LLMService {

    String END_OF_THOUGHT_SYMBOL = "<EOT>";

    private Flux<String> getStreamFromTokenStream(TokenStream tokenStream){
        return Flux.create(emitter ->{
            AtomicBoolean signalEndOfThought = new AtomicBoolean(false);

            tokenStream
                    .onToolExecuted(toolsExecution -> {
                        if(signalEndOfThought.get()){
                            emitter.next(LLMService.END_OF_THOUGHT_SYMBOL);
                        }else{
                            signalEndOfThought.set(true);
                        }
                        emitter.next("%s : %s%n%s".formatted(toolsExecution.request().name(), toolsExecution.request().arguments(), toolsExecution.result()));
                    })
                    .onRetrieved(contents -> {
                        if(signalEndOfThought.get()){
                            emitter.next(LLMService.END_OF_THOUGHT_SYMBOL);
                        }else{
                            signalEndOfThought.set(true);
                        }
                        emitter.next("Retrieved context for query%n%s".formatted(contents.stream()
                                .map(content -> "\n" + content.metadata().toString() + "\n" + content.textSegment().text())
                                .reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
                                .toString())
                        );
                    })
                    .onPartialResponse(partialResponse -> {
                        if (signalEndOfThought.get()) {
                            signalEndOfThought.set(false);
                            emitter.next(LLMService.END_OF_THOUGHT_SYMBOL);
                        }
                        emitter.next(partialResponse);
                    })
                    .onCompleteResponse(completeResponse -> emitter.complete())
                    .onError(emitter::error)
                    .start();
        });
    }

    default Flux<String> chatStream(ContentRetriever retriever, Tools tools, String sessionId, String question){
        return getStreamFromTokenStream(chatTokenStream(retriever, tools, sessionId, question));
    }

    default Flux<String> chatStream(Tools tools, String sessionId, String question){
        return getStreamFromTokenStream(chatTokenStream(tools, sessionId, question));
    }

    default Flux<String> chatStream(ContentRetriever contentRetriever, String sessionId, String question){
        return getStreamFromTokenStream(chatTokenStream(contentRetriever, sessionId, question));
    }

    TokenStream chatTokenStream(String sessionId, String question);
    TokenStream chatTokenStream(ContentRetriever contentRetriever, String sessionId, String question);
    TokenStream chatTokenStream(Tools tools, String sessionId, String question);
    TokenStream chatTokenStream(ContentRetriever retriever, Tools tools, String sessionId, String question);

    Flux<String> chatStream(String sessionId, String question);
}
