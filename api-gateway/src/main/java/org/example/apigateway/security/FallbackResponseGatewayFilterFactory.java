package org.example.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class FallbackResponseGatewayFilterFactory extends AbstractGatewayFilterFactory<FallbackResponseGatewayFilterFactory.Config> {

    public FallbackResponseGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        int statusCode = validateStatusCode(config.status());
        String message = config.message() != null ? config.message() : "{\"code\":\"NOT_FOUND\",\"message\":\"Resource not found\"}";
        
        return (exchange, _) -> {
            log.debug("Fallback response triggered for path: {}, status: {}", 
                    exchange.getRequest().getURI().getPath(), statusCode);
            exchange.getResponse().setStatusCode(HttpStatus.valueOf(statusCode));
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(message.getBytes()))
            );
        };
    }

    private int validateStatusCode(int status) {
        if (status < 100 || status > 599) {
            log.warn("Invalid HTTP status code: {}, using 404 instead", status);
            return 404;
        }
        return status;
    }

    public record Config(int status, String message) {
    }
}