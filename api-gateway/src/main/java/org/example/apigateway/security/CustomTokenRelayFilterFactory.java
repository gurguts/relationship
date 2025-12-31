package org.example.apigateway.security;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.config.SecurityConstants;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomTokenRelayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public CustomTokenRelayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(@NonNull Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String token = exchange.getAttribute(SecurityConstants.TOKEN_ATTRIBUTE);

            if (token == null) {
                token = TokenExtractor.extractFromRequest(request);
                if (TokenExtractor.isValidTokenFormat(token)) {
                    exchange.getAttributes().put(SecurityConstants.TOKEN_ATTRIBUTE, token);
                }
            }

            if (TokenExtractor.isValidTokenFormat(token)) {
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("Authorization", SecurityConstants.BEARER_PREFIX + token)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }

            return chain.filter(exchange);
        };
    }
}