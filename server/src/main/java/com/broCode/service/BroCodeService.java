package com.broCode.service;

import com.broCode.agent.BroCodeAgent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@AllArgsConstructor
public class BroCodeService {

    private final ChatSessionService chatSessionService;
    private final BroCodeAgent broCodeAgent;

    public String createNewBroCodeChatSession(){
        return chatSessionService.startNewChatSession(null, PromptService.BRO_CODE_SYSTEM_PROMPT);
    }

    public Flux<String> getBroCodeAgentResponse(String sessionId, String question){
        return broCodeAgent.chatStream(sessionId, question);
    }
}
