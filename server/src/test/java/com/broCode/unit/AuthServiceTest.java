package com.broCode.unit;

import com.broCode.dto.AuthRequest;
import com.broCode.dto.RegisterRequest;
import com.broCode.exception.InvalidCredentialsException;
import com.broCode.model.User;
import com.broCode.repository.UserRepository;
import com.broCode.service.AuthService;
import com.broCode.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    @Test
    void register_encodesPasswordAndSavesUser() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("bro@example.com");
        req.setUsername("brodev");
        req.setPassword("plaintext");

        when(passwordEncoder.encode("plaintext")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("bro@example.com");
        assertThat(saved.getUsername()).isEqualTo("brodev");
        assertThat(saved.getPassword()).isEqualTo("hashed");
    }

    @Test
    void login_byEmail_returnsTokenAndUsername() {
        User user = new User();
        user.setEmail("bro@example.com");
        user.setUsername("brodev");
        user.setPassword("hashed");

        when(userRepository.findByEmail("bro@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plaintext", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthRequest req = new AuthRequest();
        req.setIdentifier("bro@example.com");
        req.setPassword("plaintext");

        AuthService.LoginResult result = authService.login(req);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.username()).isEqualTo("brodev");
    }

    @Test
    void login_byUsername_returnsTokenAndUsername() {
        User user = new User();
        user.setEmail("bro@example.com");
        user.setUsername("brodev");
        user.setPassword("hashed");

        when(userRepository.findByEmail("brodev")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("brodev")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(eq("plaintext"), any())).thenReturn(true);
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthRequest req = new AuthRequest();
        req.setIdentifier("brodev");
        req.setPassword("plaintext");

        AuthService.LoginResult result = authService.login(req);
        assertThat(result.username()).isEqualTo("brodev");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentialsException() {
        User user = new User();
        user.setPassword("hashed");

        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        AuthRequest req = new AuthRequest();
        req.setIdentifier("bro@example.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownIdentifier_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        AuthRequest req = new AuthRequest();
        req.setIdentifier("nobody");
        req.setPassword("anything");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getUsername_existingUser_returnsUsername() {
        User user = new User();
        user.setUsername("brodev");
        when(userRepository.findById("user-id-123")).thenReturn(Optional.of(user));

        assertThat(authService.getUsername("user-id-123")).isEqualTo("brodev");
    }
}
