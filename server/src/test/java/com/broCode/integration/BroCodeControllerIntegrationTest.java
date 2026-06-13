package com.broCode.integration;

import com.broCode.dto.ChatSessionResponse;
import com.broCode.service.BroCodeService;
import com.broCode.service.ChatPersistenceService;
import com.broCode.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost/test",
        "spring.data.mongodb.auto-index-creation=true",
        "de.flapdoodle.mongodb.embedded.version=7.0.5",
        "jwt.secret=test-secret-key-at-least-32-chars-long-padding-yes-1234",
        "jwt.expiration=3600000",
        "gemini.api.key=fake-test-api-key",
        "chat.session.ttl-minutes=30",
        "frontend.allowed-origins=http://localhost:8080"
})
class BroCodeControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;

    @MockBean BroCodeService broCodeService;
    @MockBean ChatPersistenceService chatPersistenceService;

    private String validToken() {
        return jwtUtil.generateToken("test-user-id");
    }

    // ── Auth guard ────────────────────────────────────────────────────────────

    @Test
    void broCode_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/bro/broCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Hello\",\"sessionId\":null}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSessions_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/bro/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteSession_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/bro/session/some-id"))
                .andExpect(status().isUnauthorized());
    }

    // ── Authenticated endpoints ───────────────────────────────────────────────

    @Test
    void broCode_withValidJwt_returns200SseStream() throws Exception {
        when(broCodeService.createNewChatSession(any(), any())).thenReturn("sess-1");
        when(broCodeService.getBroCodeAgentResponse(any(), any())).thenReturn(Flux.just("Hello, bro!"));

        var asyncResult = mockMvc.perform(post("/api/bro/broCode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is Java?\",\"sessionId\":null}")
                        .cookie(new MockCookie("token", validToken())))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void getSessions_withValidJwt_returns200() throws Exception {
        when(chatPersistenceService.getUserSessions(any())).thenReturn(List.of(
                new ChatSessionResponse("sess-1", "React question", List.of(), 1_000_000L)
        ));

        mockMvc.perform(get("/api/bro/sessions")
                        .cookie(new MockCookie("token", validToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("sess-1"))
                .andExpect(jsonPath("$[0].title").value("React question"));
    }

    @Test
    void deleteSession_withValidJwt_returns200() throws Exception {
        mockMvc.perform(delete("/api/bro/session/sess-1")
                        .cookie(new MockCookie("token", validToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
