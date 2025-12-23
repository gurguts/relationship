package org.example.webapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import java.util.List;

/**
 * The component provides methods for creating, validating, and resolving tokens.
 */
@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKeyConfig secretKeyConfig;
    private final JwtParser jwtParser;

    public JwtTokenProvider(SecretKeyConfig secretKeyConfig) {
        this.secretKeyConfig = secretKeyConfig;
        this.jwtParser = Jwts.parser()
                .verifyWith(getSigningKey())
                .build();
    }


    private SecretKey getSigningKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyConfig.getSecretKey());
        return Keys.hmacShaKeyFor(decodedKey);
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            String username = claims.getSubject();

            List<GrantedAuthority> authorities = new ArrayList<>();
            Object authoritiesClaim = claims.get("authorities");

            if (authoritiesClaim instanceof List<?>) {
                for (Object authority : (List<?>) authoritiesClaim) {
                    if (authority instanceof String) {
                        authorities.add(new SimpleGrantedAuthority((String) authority));
                    }
                }
            } else {
                throw new WebAppException("INVALID_TOKEN_CLAIM", "Authorities claim is not a list of strings");
            }

            return new UsernamePasswordAuthenticationToken(username, "", authorities);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Failed to get authentication from token: {}", e.getMessage());
            throw new WebAppException("INVALID_TOKEN", "Invalid token: " + e.getMessage());
        }
    }

}