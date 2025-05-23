package org.example.apigateway.security;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.example.apigateway.exceptions.JwtAuthenticationException;
import org.example.apigateway.models.dto.UserDTO;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReactiveJwtTokenProvider {
    private final SecretKey secretKey;

    public Mono<UserDTO> validateToken(String token) {
        try {
            JwtParser jwtParser = Jwts.parser().verifyWith(secretKey).build();
            Jws<Claims> claimsJws = jwtParser.parseSignedClaims(token);
            Claims claims = claimsJws.getPayload();

            String username = claims.getSubject();
            List<?> rawAuthorities = claims.get("authorities", List.class);
            List<String> authorities = rawAuthorities != null
                    ? rawAuthorities.stream().map(Object::toString).toList()
                    : Collections.emptyList();

            return Mono.just(new UserDTO(username, authorities));
        } catch (JwtException e) {
            return Mono.error(new JwtAuthenticationException("Invalid or expired token"));
        }
    }
}
