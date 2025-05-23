package org.example.apigateway.security;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FallbackResponseGatewayFilterFactory extends AbstractGatewayFilterFactory<FallbackResponseGatewayFilterFactory.Config> {

    public FallbackResponseGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, _) -> {
            exchange.getResponse().setStatusCode(HttpStatus.valueOf(config.getStatus()));
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(config.getMessage().getBytes()))
            );
        };
    }

    @Data
    public static class Config {
        private int status;
        private String message;
    }
}