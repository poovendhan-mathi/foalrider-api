package com.foalrider.config;

import com.foalrider.security.CustomUserDetailsService;
import com.foalrider.security.jwt.JwtAuthEntryPoint;
import com.foalrider.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Public endpoints that don't require authentication.
     */
    private static final String[] PUBLIC_URLS = {
            // Auth endpoints
            "/auth/**",
            
            // Webhooks (Stripe needs to call without auth)
            "/webhooks/**",
            
            // Swagger/OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            
            // Actuator health
            "/actuator/health",
            "/actuator/info",
            
            // Error
            "/error"
    };

    /**
     * Public GET endpoints (read-only access).
     * Note: These work with the context-path /api/v1
     */
    private static final String[] PUBLIC_GET_URLS = {
            "/products/**",
            "/categories/**",
            "/brands/**",
            "/reviews/**",
            "/pricing/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (using JWT)
                .csrf(AbstractHttpConfigurer::disable)
                
                // Exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )
                
                // Session management - stateless
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        
                        // Public GET endpoints
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
                        
                        // Stripe webhook (has its own verification)
                        .requestMatchers("/payments/webhook").permitAll()
                        
                        // Admin endpoints
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // Authentication provider
                .authenticationProvider(authenticationProvider())
                
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
