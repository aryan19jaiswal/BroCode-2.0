package com.broCode.controller;

import com.broCode.dto.QuestionDto;
import com.broCode.dto.ResponseDto;
import com.broCode.service.BroCodeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Streaming chat endpoint.
 *
 * Returns Flux<ResponseDto> directly (not wrapped in ResponseEntity) so that
 * Spring's SSE infrastructure handles the streaming lifecycle cleanly.
 * Exceptions propagate to GlobalExceptionHandler or through the Flux error signal.
 *
 * Security note: This endpoint is protected by the JWT filter chain.
 * The AuthenticationEntryPoint handles unauthenticated requests BEFORE the
 * response is committed, eliminating the "response already committed" error.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/bro")
public class BroCodeController {

    private final BroCodeService broCodeService;

    @PostMapping(value = "/broCode", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseDto> getBroCode(@RequestBody QuestionDto questionDto) {
        log.info("Received chat request — sessionId: {}",
                questionDto.getSessionId() == null ? "new" : questionDto.getSessionId());

        String sessionId = (questionDto.getSessionId() == null || questionDto.getSessionId().isBlank())
                ? broCodeService.createNewBroCodeChatSession()
                : questionDto.getSessionId();

        return broCodeService.getBroCodeAgentResponse(sessionId, questionDto.getQuestion())
                .map(content -> new ResponseDto(content, sessionId))
                .doOnError(err -> log.error("Stream error for session {}: {}", sessionId, err.getMessage()));
    }
}