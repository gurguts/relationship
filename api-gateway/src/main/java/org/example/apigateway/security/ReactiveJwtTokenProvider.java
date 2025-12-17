package org.example.apigateway.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.apigateway.exceptions.JwtAuthenticationException;
import org.example.apigateway.models.dto.UserDTO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReactiveJwtTokenProvider {
    private final SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    public Mono<UserDTO> validateToken(String token) {
        try {
            Jws<Claims> claimsJws = jwtParser.parseSignedClaims(token);
            Claims claims = claimsJws.getPayload();

            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                return Mono.error(new JwtAuthenticationException("Token has expired"));
            }

            String username = claims.getSubject();
            if (username == null || username.isEmpty()) {
                return Mono.error(new JwtAuthenticationException("Token subject is missing"));
            }

            List<?> rawAuthorities = claims.get("authorities", List.class);
            List<String> authorities = rawAuthorities != null
                    ? rawAuthorities.stream().map(Object::toString).toList()
                    : Collections.emptyList();

            return Mono.just(new UserDTO(username, authorities));
        } catch (ExpiredJwtException e) {
            return Mono.error(new JwtAuthenticationException("Token has expired"));
        } catch (JwtException e) {
            return Mono.error(new JwtAuthenticationException("Invalid token: " + e.getMessage()));
        }
    }
}
