package com.payflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.service.MetricsService;
import com.payflow.service.RateLimitService;
import com.payflow.service.RateLimitService.RateLimitResult;
import com.payflow.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private MetricsService metricsService;
    @Mock
    private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(rateLimitService, jwtUtil, objectMapper, metricsService);
    }

    @Test
    void allowsRequestAndSetsHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.isAllowed("ip:127.0.0.1"))
                .thenReturn(new RateLimitResult(true, 100, 99, 0));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
    }

    @Test
    void returns429WhenRateLimited() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(rateLimitService.isAllowed("ip:127.0.0.1"))
                .thenReturn(new RateLimitResult(false, 100, 0, 30));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
    }

    @Test
    void usesUserIdWhenJwtPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isValid("valid-token")).thenReturn(true);
        when(jwtUtil.getUserId("valid-token")).thenReturn("42");
        when(rateLimitService.isAllowed("user:42"))
                .thenReturn(new RateLimitResult(true, 100, 99, 0));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).isAllowed("user:42");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void fallsBackToIpWhenJwtInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isValid("bad-token")).thenReturn(false);
        when(rateLimitService.isAllowed("ip:10.0.0.1"))
                .thenReturn(new RateLimitResult(true, 100, 99, 0));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).isAllowed("ip:10.0.0.1");
    }
}
