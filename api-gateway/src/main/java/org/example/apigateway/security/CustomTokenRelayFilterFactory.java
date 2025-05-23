package org.example.apigateway.security;

import lombok.extern.slf4j.Slf4j;
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
            String token = extractTokenFromRequest(request);

            if (token != null && !token.isEmpty()) {
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("Authorization", String.format("Bearer %s", token))
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            return chain.filter(exchange);
        };
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(auth -> auth.startsWith("Bearer "))
                .map(auth -> auth.substring(7))
                .or(() -> Optional.ofNullable(request.getCookies().getFirst("authToken"))
                        .map(HttpCookie::getValue))
                .orElse(null);
    }
}