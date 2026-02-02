package com.payflow.service;

import com.payflow.dto.*;
import com.payflow.exception.EmailAlreadyExistsException;
import com.payflow.exception.InvalidCredentialsException;
import com.payflow.model.User;
import com.payflow.repository.UserRepository;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "hashedpw", "Test User");
        testUser.setId(1L);
    }

    @Test
    void registerSuccessfully() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedpw");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(1L, "test@example.com")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L, "test@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.register(new RegisterRequest("test@example.com", "password123", "Test User"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void registerThrowsOnDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("test@example.com", "password123", "Test User")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginSuccessfully() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpw")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L, "test@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "password123"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void loginThrowsOnBadEmail() {
        when(userRepository.findByEmail("bad@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("bad@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginThrowsOnBadPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpw", "hashedpw")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrongpw")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshSuccessfully() {
        when(jwtUtil.isValid("refresh-token")).thenReturn(true);
        when(jwtUtil.getTokenType("refresh-token")).thenReturn("refresh");
        when(jwtUtil.getUserId("refresh-token")).thenReturn("1");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateAccessToken(1L, "test@example.com")).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(1L, "test@example.com")).thenReturn("new-refresh");

        AuthResponse response = authService.refresh(new RefreshRequest("refresh-token"));

        assertThat(response.getAccessToken()).isEqualTo("new-access");
    }

    @Test
    void refreshThrowsOnInvalidToken() {
        when(jwtUtil.isValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
