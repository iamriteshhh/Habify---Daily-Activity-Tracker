package com.habify.security;

import com.habify.entity.User;
import com.habify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConfig
 * 
 * Configures Spring Security with stateless JWT validation,
 * rate limiting on auth endpoints, path authorization rules,
 * and restrictive CORS policies.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final List<String> allowedOrigins;
    private final boolean h2ConsoleEnabled;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitingFilter rateLimitingFilter,
            @Value("${habify.cors.allowed-origins:http://localhost:8080,http://127.0.0.1:8080}") List<String> allowedOrigins,
            @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.allowedOrigins = allowedOrigins;
        this.h2ConsoleEnabled = h2ConsoleEnabled;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
            .authorizeHttpRequests(auth -> {
                // Static web resources & error pages
                auth.requestMatchers(
                    "/",
                    "/index.html",
                    "/auth.html",
                    "/dashboard.html",
                    "/privacy.html",
                    "/terms.html",
                    "/404.html",
                    "/error",
                    "/error/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico"
                ).permitAll();

                // Authentication endpoints
                auth.requestMatchers("/api/auth/**").permitAll();

                // Conditionally permit H2 console only if explicitly enabled
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }

                // Protected API endpoints
                auth.requestMatchers("/api/**").authenticated();

                // Non-API web routes fall through to Spring MVC 404 handler
                auth.anyRequest().permitAll();
            })
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return usernameOrEmail -> {
            User user = userRepository.findByEmailOrUsername(usernameOrEmail, usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail));
            return new UserPrincipal(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getPasswordHash(),
                    Boolean.TRUE.equals(user.getEmailVerified())
            );
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
