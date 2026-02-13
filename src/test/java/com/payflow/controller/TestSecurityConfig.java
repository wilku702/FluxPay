package com.payflow.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Shared test security configuration imported by all {@code @WebMvcTest} controller tests.
 *
 * Defines a minimal, permissive {@code SecurityFilterChain}:
 * <ul>
 *   <li><strong>CSRF disabled</strong> — no CSRF token needed on mutating requests.</li>
 *   <li><strong>All requests permitted</strong> — authorization rules are tested at the
 *       service-layer unit test level, not in controller slice tests.</li>
 *   <li><strong>Stateless sessions</strong> — matches the production JWT configuration.</li>
 * </ul>
 *
 * {@code @Order(1)} gives this chain higher priority than the production
 * {@code SecurityConfig} filter chain (default order 2147483642), ensuring all
 * requests are routed through this permissive chain during tests.
 *
 * Authentication is still supplied per-request via
 * {@code .with(user("1"))} from {@code SecurityMockMvcRequestPostProcessors}
 * so that Spring MVC can resolve the {@code Authentication} parameter that
 * controllers inject via {@code authentication.getName()}.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
