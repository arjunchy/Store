package com.ecommerce.backend.security;

import com.ecommerce.backend.config.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey cachedSigningKey;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // Refuse to run with a weak/known signing secret: token forgery is trivial otherwise.
            throw new IllegalStateException(
                    "jwt.secret is too weak (must be >= 32 bytes, preferably 64+). "
                    + "Set a strong value via the JWT_SECRET environment variable."
            );
        }
        this.cachedSigningKey = computeSigningKey();
    }

    private SecretKey computeSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret must be >= 32 bytes. Set a strong JWT_SECRET.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        if (cachedSigningKey == null) {
            cachedSigningKey = computeSigningKey();
        }
        return cachedSigningKey;
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        log.debug("Generating access token for user: {}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("userId", userDetails.getUserId());

        String token = createToken(
                claims,
                userDetails.getUsername(),
                expiration
        );

        log.debug("Access token generated for user: {}", userDetails.getUsername());
        return token;
    }

    public String generateRefreshToken(CustomUserDetails userDetails) {
        log.debug("Generating refresh token for user: {}", userDetails.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", userDetails.getUserId());

        String token = createToken(
                claims,
                userDetails.getUsername(),
                refreshExpiration
        );

        log.debug("Refresh token generated for user: {}", userDetails.getUsername());
        return token;
    }

    private String createToken(
            Map<String, Object> claims,
            String subject,
            long expirationTime) {

        Date issuedAt = new Date();

        claims.put("jti", java.util.UUID.randomUUID().toString());

        Date expirationDate =
                new Date(issuedAt.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    public String extractUserId(String token) {
        return extractClaim(
                token,
                claims -> claims.get("userId", String.class)
        );
    }

    public boolean isTokenValid(
            String token,
            CustomUserDetails userDetails) {

        try {
            String username = extractUsername(token);
            String type = extractClaim(token, c -> c.get("type", String.class));
            // Access token must be type=access for API authentication
            if (!"access".equals(type)) {
                log.debug("Token type mismatch: expected access but got {}", type);
                return false;
            }
            return username != null
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);

        } catch (Exception e) {
            log.debug(
                    "JWT token validation failed for user {}: {}",
                    userDetails.getUsername(),
                    e.getMessage()
            );
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = extractClaim(token, c -> c.get("type", String.class));
            return "access".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {

        try {

            String type = extractClaim(
                    token,
                    claims -> claims.get("type", String.class)
            );

            return "refresh".equals(type);

        } catch (Exception e) {
            log.debug(
                    "Failed to determine if token is refresh token: {}",
                    e.getMessage()
            );
            return false;
        }
    }

    public boolean isTokenValidRefresh(String token, CustomUserDetails userDetails) {
        try {
            String username = extractUsername(token);
            String type = extractClaim(token, c -> c.get("type", String.class));
            if (!"refresh".equals(type)) return false;
            return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(
                token,
                Claims::getExpiration
        );
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return resolver.apply(claims);
    }
}