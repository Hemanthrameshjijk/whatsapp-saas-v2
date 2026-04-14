package com.whatsappai.security;

import com.whatsappai.entity.AppUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiry-hours}")
    private int expiryHours;

    public String generateToken(AppUser user) {
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("businessId", user.getBusinessId().toString())
            .claim("role", user.getRole())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiryHours * 3600_000L))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(UTF_8)))
            .compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * ALWAYS use this — businessId is NEVER accepted from request body or path param.
     * It is ALWAYS extracted from the JWT stored in auth.getDetails().
     */
    public UUID extractBusinessId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        Object details = auth.getDetails();
        if (details instanceof String bid && !bid.isBlank()) {
            return UUID.fromString(bid);
        }
        throw new AccessDeniedException("businessId not found in authentication context");
    }
}
