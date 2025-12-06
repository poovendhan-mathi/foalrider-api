package com.foalrider.security.jwt;

import com.foalrider.config.JwtConfig;
import com.foalrider.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT token provider for generating and validating JWT tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Generate access token from authentication.
     */
    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateAccessToken(userDetails);
    }

    /**
     * Generate access token from user details.
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .subject(userDetails.getId().toString())
                .claim("email", userDetails.getEmail())
                .claim("role", userDetails.getRoleName())
                .claim("firstName", userDetails.getFirstName())
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtConfig.getIssuer())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate refresh token.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get user ID from token.
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Get email from token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    /**
     * Get role from token.
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get token expiration time in seconds.
     */
    public long getAccessTokenExpirationInSeconds() {
        return jwtConfig.getAccessTokenExpiration() / 1000;
    }

    /**
     * Get refresh token expiration time in milliseconds.
     */
    public long getRefreshTokenExpiration() {
        return jwtConfig.getRefreshTokenExpiration();
    }

    /**
     * Get signing key from secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
