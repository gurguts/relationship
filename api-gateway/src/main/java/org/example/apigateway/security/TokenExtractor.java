package org.example.apigateway.security;

import lombok.NonNull;
import org.example.apigateway.config.SecurityConstants;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Optional;

final class TokenExtractor {

    private TokenExtractor() {
    }

    static String extractFromExchange(@NonNull ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
        }

        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(SecurityConstants.AUTH_TOKEN_COOKIE);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return cookie.getValue();
        }

        return null;
    }

    static String extractFromRequest(@NonNull ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(auth -> auth.startsWith(SecurityConstants.BEARER_PREFIX))
                .map(auth -> auth.substring(SecurityConstants.BEARER_PREFIX.length()))
                .or(() -> Optional.ofNullable(request.getCookies().getFirst(SecurityConstants.AUTH_TOKEN_COOKIE))
                        .map(HttpCookie::getValue))
                .orElse(null);
    }

    static boolean isValidTokenFormat(String token) {
        return token != null && !token.trim().isEmpty() && token.length() > 10;
    }
}

