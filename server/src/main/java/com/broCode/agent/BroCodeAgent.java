package com.broCode.agent;

import com.broCode.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BroCodeAgent extends Agent {

    @Autowired
    public BroCodeAgent(LLMService llmService) {
        super(llmService);
    }

}
