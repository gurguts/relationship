package org.example.apigateway.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.models.dto.ErrorResponse;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionAdvice implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;
    private static final ExceptionMapping[] EXCEPTION_MAPPINGS = ExceptionMapping.getMappings();

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
        
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        ExceptionResult result = mapException(ex, exchange);
        
        exchange.getResponse().setStatusCode(result.httpStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            String jsonResponse = objectMapper.writeValueAsString(result.errorResponse());
            DataBuffer dataBuffer = bufferFactory.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        } catch (JsonProcessingException e) {
            log.error("Error serializing error response. Path: {}, Method: {}, Remote: {}", 
                    getPath(exchange), getMethod(exchange), getRemoteAddress(exchange), e);
            ErrorResponse fallbackResponse = new ErrorResponse(
                    ErrorConstants.ERROR_SERVER_ERROR, 
                    ErrorConstants.MESSAGE_INTERNAL_SERVER_ERROR, 
                    null);
            try {
                String jsonResponse = objectMapper.writeValueAsString(fallbackResponse);
                DataBuffer dataBuffer = bufferFactory.wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
                return exchange.getResponse().writeWith(Mono.just(dataBuffer));
            } catch (JsonProcessingException ex2) {
                log.error("Critical: Unable to serialize fallback error response", ex2);
                return Mono.error(ex2);
            }
        }
    }

    private ExceptionResult mapException(Throwable ex, ServerWebExchange exchange) {
        for (ExceptionMapping mapping : EXCEPTION_MAPPINGS) {
            if (mapping.exceptionType().isInstance(ex)) {
                String path = getPath(exchange);
                String method = getMethod(exchange);
                String remoteAddress = getRemoteAddress(exchange);
                String message = Objects.toString(ex.getMessage(), "");
                
                log.warn("Exception handled. Type: {}, Message: {}, Path: {}, Method: {}, Remote: {}", 
                        mapping.exceptionType().getSimpleName(), message, path, method, remoteAddress);
                
                return new ExceptionResult(mapping.responseBuilder().apply(ex), mapping.httpStatus());
            }
        }

        String path = getPath(exchange);
        String method = getMethod(exchange);
        String remoteAddress = getRemoteAddress(exchange);
        log.error("Unexpected error occurred. Path: {}, Method: {}, Remote: {}", 
                path, method, remoteAddress, ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                ErrorConstants.ERROR_SERVER_ERROR,
                ErrorConstants.MESSAGE_INTERNAL_SERVER_ERROR,
                null);
        return new ExceptionResult(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getPath(ServerWebExchange exchange) {
        try {
            return Objects.toString(exchange.getRequest().getURI().getPath(), "unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getMethod(ServerWebExchange exchange) {
        try {
            exchange.getRequest().getMethod();
            return exchange.getRequest().getMethod().name();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getRemoteAddress(ServerWebExchange exchange) {
        try {
            return exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().toString()
                    : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private record ExceptionResult(ErrorResponse errorResponse, HttpStatus httpStatus) {
    }
}