package org.example.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.config.SecurityConstants;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class CustomTokenRelayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public CustomTokenRelayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = exchange.getAttribute(SecurityConstants.TOKEN_ATTRIBUTE);

            if (token == null) {
                token = extractTokenFromRequest(request);
                if (token != null && !token.isEmpty()) {
                    exchange.getAttributes().put(SecurityConstants.TOKEN_ATTRIBUTE, token);
                }
            }

            if (token != null && !token.isEmpty()) {
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("Authorization", SecurityConstants.BEARER_PREFIX + token)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            return chain.filter(exchange);
        };
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(auth -> auth.startsWith(SecurityConstants.BEARER_PREFIX))
                .map(auth -> auth.substring(SecurityConstants.BEARER_PREFIX.length()))
                .or(() -> Optional.ofNullable(request.getCookies().getFirst(SecurityConstants.AUTH_TOKEN_COOKIE))
                        .map(HttpCookie::getValue))
                .orElse(null);
    }
}