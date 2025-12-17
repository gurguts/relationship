package org.example.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class InvalidRequestFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        try {
            URI uri = exchange.getRequest().getURI();
            if (containsInvalidCharacters(uri.getPath()) || containsInvalidCharacters(uri.getQuery())) {
                log.warn("Request blocked due to invalid characters. Path: {}, Query: {}, Remote: {}", 
                        uri.getPath(), uri.getQuery(), exchange.getRequest().getRemoteAddress());
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(
                                "{\"code\":\"INVALID_REQUEST\",\"message\":\"Request contains invalid characters\"}".getBytes()
                        ))
                );
            }
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("Error in InvalidRequestFilter", e);
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean containsInvalidCharacters(String str) {
        if (str == null) return false;
        return str.chars().anyMatch(c -> c < 32 && c != '\t' && c != '\n' && c != '\r');
    }
}