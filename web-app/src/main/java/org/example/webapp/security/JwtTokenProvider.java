package org.example.webapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.webapp.config.SecretKeyConfig;
import org.example.webapp.exceptions.WebAppException;
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
    private final SecretKeyConfig secretKeyConfig;
    private final JwtParser jwtParser;

    public JwtTokenProvider(@NonNull SecretKeyConfig secretKeyConfig) {
        this.secretKeyConfig = secretKeyConfig;
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
            byte[] decodedKey = Base64.getDecoder().decode(secretKeyConfig.getSecret());
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

            List<GrantedAuthority> authorities = extractAuthorities(claims);

            return new UsernamePasswordAuthenticationToken(username, "", authorities);
        } catch (JwtException e) {
            log.error("Failed to get authentication from token: {}", e.getMessage());
            throw new WebAppException("INVALID_TOKEN", "Invalid token: " + e.getMessage());
        }
    }

    private List<GrantedAuthority> extractAuthorities(@NonNull Claims claims) {
        Object authoritiesClaim = claims.get(SecurityConstants.CLAIM_AUTHORITIES);
        if (authoritiesClaim == null) {
            return Collections.emptyList();
        }

        if (!(authoritiesClaim instanceof List<?>)) {
            throw new WebAppException("INVALID_TOKEN_CLAIM", "Authorities claim is not a list");
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