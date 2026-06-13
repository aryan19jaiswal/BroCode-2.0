package com.broCode.unit;

import com.broCode.exception.InvalidSessionException;
import com.broCode.service.InMemoryChatSessionService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryChatSessionServiceTest {

    private InMemoryChatSessionService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryChatSessionService();
        ReflectionTestUtils.setField(service, "sessionTtlMinutes", 30);
    }

    @Test
    void startNewChatSession_createsSessionWithSystemPrompt() {
        String sessionId = service.startNewChatSession("You are a helpful assistant.");

        assertThat(sessionId).isNotBlank();
        List<ChatMessage> messages = service.getMessages(sessionId);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) messages.get(0)).text()).isEqualTo("You are a helpful assistant.");
    }

    @Test
    void validateSessionId_unknownSession_throwsInvalidSessionException() {
        assertThatThrownBy(() -> service.validateSessionId("nonexistent-id"))
                .isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void getMessages_unknownSession_throwsInvalidSessionException() {
        assertThatThrownBy(() -> service.getMessages("nonexistent-id"))
                .isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void addChatMessage_appendsMessageToSession() {
        String sessionId = service.startNewChatSession("sys");

        service.addChatMessage(sessionId, UserMessage.from("Hello"));
        service.addChatMessage(sessionId, AiMessage.from("Hi there!"));

        List<ChatMessage> messages = service.getMessages(sessionId);
        assertThat(messages).hasSize(3); // system + user + assistant
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(2)).isInstanceOf(AiMessage.class);
    }

    @Test
    void destroyChatSession_removesSession() {
        String sessionId = service.startNewChatSession("sys");
        service.destroyChatSession(sessionId);

        assertThatThrownBy(() -> service.validateSessionId(sessionId))
                .isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void deleteMessages_clearsHistoryWithoutDestroyingSession() {
        String sessionId = service.startNewChatSession("sys");
        service.addChatMessage(sessionId, UserMessage.from("Hello"));

        service.deleteMessages(sessionId);

        // Session still exists but history is empty
        service.validateSessionId(sessionId); // no exception
        assertThat(service.getMessages(sessionId)).isEmpty();
    }

    @Test
    void restoreSession_createsSessionFromExistingMessages() {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("sys"),
                UserMessage.from("question"),
                AiMessage.from("answer")
        );

        String sessionId = "restored-session-id";
        service.restoreSession(sessionId, messages);

        service.validateSessionId(sessionId); // no exception
        assertThat(service.getMessages(sessionId)).hasSize(3);
    }

    @Test
    void evictExpiredSessions_removesSessionsBeyondTtl() throws InterruptedException {
        ReflectionTestUtils.setField(service, "sessionTtlMinutes", 0);
        String sessionId = service.startNewChatSession("sys");

        Thread.sleep(5); // ensure last-access time is before the cutoff
        service.evictExpiredSessions();

        assertThatThrownBy(() -> service.validateSessionId(sessionId))
                .isInstanceOf(InvalidSessionException.class);
    }

    @Test
    void getChatHistory_returnsUnmodifiableList() {
        String sessionId = service.startNewChatSession("sys");
        List<ChatMessage> history = service.getMessages(sessionId);

        // The returned list must be unmodifiable
        assertThatThrownBy(() -> history.add(UserMessage.from("should fail")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
