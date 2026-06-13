package com.broCode.integration;

import com.broCode.model.User;
import com.broCode.repository.UserRepository;
import com.broCode.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired JwtUtil jwtUtil;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bro@example.com","username":"brodev","password":"securepass"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = """
                {"email":"dup@example.com","username":"brodev","password":"securepass"}
                """;
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second registration with the same email must be rejected
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@example.com","username":"other","password":"securepass"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void register_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","username":"brodev","password":"securepass"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200AndSetsCookie() throws Exception {
        User user = new User();
        user.setEmail("bro@example.com");
        user.setUsername("brodev");
        user.setPassword(passwordEncoder.encode("securepass"));
        userRepository.save(user);

        var result = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"bro@example.com","password":"securepass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value("brodev"))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull().contains("token=");
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        User user = new User();
        user.setEmail("bro@example.com");
        user.setUsername("brodev");
        user.setPassword(passwordEncoder.encode("securepass"));
        userRepository.save(user);

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"bro@example.com","password":"wrongpass"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Test
    void getProfile_withValidJwt_returns200AndUsername() throws Exception {
        User user = new User();
        user.setEmail("bro@example.com");
        user.setUsername("brodev");
        user.setPassword(passwordEncoder.encode("securepass"));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId());

        mockMvc.perform(get("/api/user/profile")
                        .cookie(new MockCookie("token", token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("brodev"));
    }

    @Test
    void getProfile_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_returns200AndClearsCookieMaxAge() throws Exception {
        var result = mockMvc.perform(post("/api/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull().contains("Max-Age=0");
    }
}
