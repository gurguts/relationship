package org.example.apigateway.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.exceptions.JwtAuthenticationException;
import org.example.apigateway.models.dto.ErrorResponse;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global exception handler for WebFlux reactive stack.
 * This handler processes exceptions that occur during request processing in Spring Cloud Gateway.
 * Order(-2) ensures it runs before the default error handler but after security filters.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionAdvice implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ErrorResponse errorResponse = buildErrorResponse(ex);
        HttpStatus httpStatus = determineHttpStatus(ex);
        
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            DataBuffer dataBuffer = bufferFactory.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response", e);
            DataBuffer dataBuffer = bufferFactory.wrap(
                    "{\"error\":\"SERVER_ERROR\",\"message\":\"Internal server error\"}".getBytes(StandardCharsets.UTF_8)
            );
            return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        }
    }

    private ErrorResponse buildErrorResponse(Throwable ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            log.warn("Bad request: {}", ex.getMessage());
            return new ErrorResponse("BAD_REQUEST", ex.getMessage(), null);
        }

        if (ex instanceof NoResourceFoundException) {
            log.warn("Resource not found: {}", ex.getMessage());
            return new ErrorResponse("NOT_FOUND", "Resource not found", null);
        }

        if (ex instanceof AccessDeniedException) {
            log.warn("Access Denied: {}", ex.getMessage());
            return new ErrorResponse("ACCESS_DENIED", 
                    "You do not have permission to perform this action.", null);
        }

        if (ex instanceof AuthenticationException || ex instanceof JwtAuthenticationException) {
            log.warn("Authentication failed: {}", ex.getMessage());
            return new ErrorResponse("UNAUTHORIZED", 
                    ex.getMessage() != null ? ex.getMessage() : "Authentication failed", null);
        }

        log.error("An unexpected error occurred", ex);
        return new ErrorResponse("SERVER_ERROR", "Internal server error", null);
    }

    private HttpStatus determineHttpStatus(Throwable ex) {
        if (ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
            return HttpStatus.BAD_REQUEST;
        }

        if (ex instanceof NoResourceFoundException) {
            return HttpStatus.NOT_FOUND;
        }

        if (ex instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }

        if (ex instanceof AuthenticationException || ex instanceof JwtAuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}