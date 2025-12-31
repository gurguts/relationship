package org.example.clientservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {
    private final String secretKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(@NonNull String secretKey) {
        if (secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret key cannot be null or empty");
        }
        this.secretKey = secretKey;
        try {
            this.jwtParser = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT parser", e);
        }
    }

    private SecretKey getSigningKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secretKey);
            return Keys.hmacShaKeyFor(decodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid secret key format", e);
        }
    }

    public boolean validateToken(@NonNull String token) {
        if (token.trim().isEmpty()) {
            return false;
        }
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(@NonNull String token) {
        if (token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be empty");
        }

        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            String username = claims.getSubject();
            if (username == null || username.isEmpty()) {
                throw new IllegalArgumentException("Token subject is missing");
            }

            Long userId = claims.get(SecurityConstants.CLAIM_USER_ID, Long.class);
            List<GrantedAuthority> authorities = extractAuthorities(claims);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, token, authorities);
            authentication.setDetails(userId);

            return authentication;
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid token: " + e.getMessage(), e);
        }
    }

    private List<GrantedAuthority> extractAuthorities(@NonNull Claims claims) {
        Object authoritiesClaim = claims.get(SecurityConstants.CLAIM_AUTHORITIES);
        if (authoritiesClaim == null) {
            return Collections.emptyList();
        }

        if (!(authoritiesClaim instanceof List<?>)) {
            throw new IllegalArgumentException("Authorities claim is not a list");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Object authority : (List<?>) authoritiesClaim) {
            if (authority instanceof String authorityString && !authorityString.trim().isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(authorityString));
            }
        }

        return authorities;
    }
}
