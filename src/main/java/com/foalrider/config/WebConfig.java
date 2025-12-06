package com.foalrider.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Web configuration including CORS settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // Set allowed origins
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        corsConfiguration.setAllowedOrigins(origins);
        
        // Set allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        corsConfiguration.setAllowedMethods(methods);
        
        // Set allowed headers
        if ("*".equals(allowedHeaders)) {
            corsConfiguration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            corsConfiguration.setAllowedHeaders(headers);
        }
        
        // Set credentials
        corsConfiguration.setAllowCredentials(allowCredentials);
        
        // Set max age
        corsConfiguration.setMaxAge(maxAge);
        
        // Expose headers for frontend to access
        corsConfiguration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        return new CorsFilter(source);
    }
}
