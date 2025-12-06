package com.foalrider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * Secret key for signing JWT tokens.
     * Should be at least 512 bits for HS512 algorithm.
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     * Default: 15 minutes (900000ms)
     */
    private long accessTokenExpiration = 900000;

    /**
     * Refresh token expiration time in milliseconds.
     * Default: 7 days (604800000ms)
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * JWT token issuer.
     */
    private String issuer = "foalrider-api";
}
