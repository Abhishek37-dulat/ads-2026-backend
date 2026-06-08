package com.relay.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Issues and validates self-signed HS256 session tokens. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlDays;

    public JwtService(
        @Value("${relay.auth.jwt-secret}") String secret,
        @Value("${relay.auth.jwt-ttl-days:7}") long ttlDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlDays = ttlDays;
    }

    public String issue(UUID userId, String email, String name, UUID workspaceId, String workspaceName, boolean admin) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("name", name)
            .claim("workspace", workspaceId.toString())
            .claim("workspaceName", workspaceName)
            .claim("admin", admin)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(ttlDays, ChronoUnit.DAYS)))
            .signWith(key)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
