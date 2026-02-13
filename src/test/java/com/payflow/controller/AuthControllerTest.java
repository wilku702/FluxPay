package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.dto.AuthResponse;
import com.payflow.dto.LoginRequest;
import com.payflow.dto.RegisterRequest;
import com.payflow.exception.InvalidCredentialsException;
import com.payflow.service.AuthService;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for {@link AuthController}.
 *
 * {@link TestSecurityConfig} provides a minimal, permissive {@code SecurityFilterChain}
 * that replaces the production config — CSRF disabled, no JWT filter, all requests
 * permitted — so auth endpoints can be called without tokens.
 *
 * Only {@link AuthService} needs to be mocked; all other infrastructure beans
 * ({@code JwtUtil}, {@code JwtAuthenticationFilter}) are satisfied by the
 * test {@code application.yml} and the {@link TestSecurityConfig} respectively.
 */
@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    /**
     * {@code JwtUtil} lives in the {@code util} package which is outside the
     * {@code @WebMvcTest} component-scan scope, so it is not auto-detected.
     * {@code JwtAuthenticationFilter} (in the {@code config} package) is
     * auto-detected and needs a {@code JwtUtil} bean to satisfy its constructor.
     * Providing a mock here satisfies that dependency without requiring real JWT
     * configuration. The filter itself runs but never sees a Bearer header in
     * these tests, so it simply passes the request through.
     */
    @MockBean
    private JwtUtil jwtUtil;

    private static final AuthResponse STUB_AUTH_RESPONSE =
            new AuthResponse("access-token", "refresh-token", 1L, "test@example.com", "Test User");

    // -------------------------------------------------------------------------
    // POST /api/auth/register
    // -------------------------------------------------------------------------

    @Test
    void registerReturns201WithValidRequest() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(STUB_AUTH_RESPONSE);

        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Test User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void registerReturns400WithInvalidRequest() throws Exception {
        // Empty body — all @NotBlank constraints fire
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturns400WithMissingEmailField() throws Exception {
        // Blank email and short password both violate constraints
        RegisterRequest request = new RegisterRequest("", "pw", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------

    @Test
    void loginReturns200WithValidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(STUB_AUTH_RESPONSE);

        LoginRequest request = new LoginRequest("test@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void loginReturns401WithBadCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
