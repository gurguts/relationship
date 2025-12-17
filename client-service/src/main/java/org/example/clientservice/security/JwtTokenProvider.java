package org.example.clientservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.config.SecretKeyConfig;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {
    private final String secretKey;
    private final JwtParser jwtParser;

    public JwtTokenProvider(String secretKey) {
        this.secretKey = secretKey;
        this.jwtParser = Jwts.parser()
                .verifyWith(getSigningKey())
                .build();
    }


    private SecretKey getSigningKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        List<GrantedAuthority> authorities = new ArrayList<>();
        Object authoritiesClaim = claims.get("authorities");

        if (authoritiesClaim instanceof List<?>) {
            for (Object authority : (List<?>) authoritiesClaim) {
                if (authority instanceof String) {
                    authorities.add(new SimpleGrantedAuthority((String) authority));
                }
            }
        } else {
            throw new IllegalArgumentException("Authorities claim is not a list of strings");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, token, authorities);
        authentication.setDetails(userId);

        return authentication;
    }
}