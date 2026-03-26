package com.mkwang.backend.config;

import com.mkwang.backend.modules.auth.security.JwtAuthenticationEntryPoint;
import com.mkwang.backend.modules.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AuthenticationProvider authenticationProvider;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        // Public endpoints — change-password, me, logout yêu cầu authenticated
        private static final String[] WHITE_LIST_URLS = {
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh-token",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/test/public",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**",
                        // WebSocket handshake — JWT auth handled at STOMP channel level
                        "/ws/**"
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // Disable CSRF for stateless API
                                .csrf(AbstractHttpConfigurer::disable)

                                // Configure CORS
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // Configure authorization
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(WHITE_LIST_URLS).permitAll()
                                                .anyRequest().authenticated())

                                // Configure session management - stateless for JWT
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Configure exception handling
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                                // Set authentication provider
                                .authenticationProvider(authenticationProvider)

                                // Add JWT filter before UsernamePasswordAuthenticationFilter
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("*")); // Configure as needed
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
