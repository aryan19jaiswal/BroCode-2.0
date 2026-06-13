package com.broCode.controller;

import com.broCode.dto.ChatSessionResponse;
import com.broCode.dto.QuestionDto;
import com.broCode.dto.ResponseDto;
import com.broCode.service.BroCodeService;
import com.broCode.service.ChatPersistenceService;
import com.broCode.service.ChatSessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * Streaming chat endpoint + session management REST endpoints.
 *
 * All endpoints require authentication — the JWT filter and Spring Security
 * ensure requests without a valid cookie never reach this controller.
 */
@Slf4j
@RestController
@RequestMapping("/api/bro")
public class BroCodeController {

    private final BroCodeService broCodeService;
    private final ChatPersistenceService chatPersistenceService;
    private final ChatSessionService chatSessionService;

    public BroCodeController(BroCodeService broCodeService,
                             ChatPersistenceService chatPersistenceService,
                             ChatSessionService chatSessionService) {
        this.broCodeService = broCodeService;
        this.chatPersistenceService = chatPersistenceService;
        this.chatSessionService = chatSessionService;
    }

    @PostMapping(value = "/broCode", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ResponseDto> getBroCode(@Valid @RequestBody QuestionDto questionDto) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        final String sessionId;
        if (questionDto.getSessionId() == null || questionDto.getSessionId().isBlank()) {
            String title = questionDto.getQuestion().length() > 50
                    ? questionDto.getQuestion().substring(0, 50) + "…"
                    : questionDto.getQuestion();
            sessionId = broCodeService.createNewChatSession(userId, title);
            log.info("New session {} for user {}", sessionId, userId);
        } else {
            sessionId = broCodeService.resolveSession(questionDto.getSessionId(), userId);
            log.info("Resumed session {} for user {}", sessionId, userId);
        }

        return broCodeService.getBroCodeAgentResponse(sessionId, questionDto.getQuestion())
                .map(content -> new ResponseDto(content, sessionId))
                .doOnError(err -> log.error("Stream error for session {}: {}", sessionId, err.getMessage()))
                .doFinally(signal ->
                        Mono.fromRunnable(() -> broCodeService.syncSessionToMongo(sessionId))
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe()
                );
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getSessions() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(chatPersistenceService.getUserSessions(userId));
    }

    @DeleteMapping("/session/{id}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String id) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        chatPersistenceService.deleteSession(id, userId);
        chatSessionService.destroyChatSession(id);
        return ResponseEntity.ok(Map.of("message", "Session deleted", "success", true));
    }
}
