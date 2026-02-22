package com.broCode.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Utility class for handling JSON Web Token (JWT) operations.
 * Responsible for generating, signing, parsing, and validating tokens.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.expiration}")
    private long expirationTime;

    private SecretKey key;

    /**
     * Initializes the HMAC SHA key using the secret string provided in properties.
     * Executed after dependency injection.
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        log.info("JwtUtil initialized with secret key.");
    }

    /**
     * Generates a new JWT for the specified username.
     *
     * @param username The subject of the token.
     * @return A signed JWT string.
     */
    public String generateToken(String username) {
        log.debug("Generating token for user: {}", username);
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts the username (subject) from a given token.
     *
     * @param token The JWT string.
     * @return The username contained in the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Alias for extractUsername, kept for backward compatibility/clarity.
     *
     * @param token The JWT string.
     * @return The subject (User ID/Name) from the token.
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a given token.
     *
     * @param token The JWT string.
     * @return The expiration Date object.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract a specific claim from the token.
     *
     * @param token          The JWT string.
     * @param claimsResolver A function to resolve the claim.
     * @param <T>            The type of the claim.
     * @return The extracted claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the token to retrieve all claims/payload data.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if the token's expiration date matches the current time.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates if the token signature is correct and if the token is not expired.
     *
     * @param token The JWT string to validate.
     * @return true if valid, false otherwise.
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
