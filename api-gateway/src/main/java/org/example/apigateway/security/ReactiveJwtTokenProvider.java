package org.example.apigateway.security;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.apigateway.exceptions.JwtAuthenticationException;
import org.example.apigateway.models.dto.UserDTO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public Mono<UserDTO> validateToken(@NonNull String token) {
        if (token.trim().isEmpty()) {
            return Mono.error(new JwtAuthenticationException("Token cannot be empty"));
        }

        try {
            Jws<Claims> claimsJws = jwtParser.parseSignedClaims(token);
            Claims claims = claimsJws.getPayload();

            String username = claims.getSubject();
            if (username == null || username.isEmpty()) {
                return Mono.error(new JwtAuthenticationException("Token subject is missing"));
            }

            List<String> authorities = extractAuthorities(claims);

            return Mono.just(new UserDTO(username, authorities));
        } catch (ExpiredJwtException e) {
            return Mono.error(new JwtAuthenticationException("Token has expired"));
        } catch (JwtException e) {
            return Mono.error(new JwtAuthenticationException("Invalid token: " + e.getMessage()));
        }
    }

    private List<String> extractAuthorities(Claims claims) {
        Object rawAuthorities = claims.get("authorities");
        if (rawAuthorities == null) {
            return Collections.emptyList();
        }

        if (rawAuthorities instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
