package org.example.apigateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.apigateway.models.dto.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvalidRequestFilter implements WebFilter {
    private static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    private static final String MESSAGE_KEY_INVALID_REQUEST = "invalid.request";

    private static final String ALLOWED_CHARS_PATH = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=%";
    private static final String ALLOWED_CHARS_QUERY = ALLOWED_CHARS_PATH + "{}\"\\ ";

    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        try {
            URI uri = exchange.getRequest().getURI();
            String path = uri.getPath();
            String query = uri.getQuery();
            
            boolean pathInvalid = containsInvalidCharacters(path, ALLOWED_CHARS_PATH);
            boolean queryInvalid = containsInvalidCharacters(query, ALLOWED_CHARS_QUERY);
            
            if (pathInvalid || queryInvalid) {
                if (pathInvalid) {
                    log.warn("Request blocked due to invalid characters in path. Path: {}, Remote: {}", 
                            path, exchange.getRequest().getRemoteAddress());
                }
                if (queryInvalid) {
                    log.warn("Request blocked due to invalid characters in query. Query: {}, Remote: {}", 
                            query, exchange.getRequest().getRemoteAddress());
                }
                
                Locale locale = getLocaleFromRequest(exchange);
                String localizedMessage = messageSource.getMessage(
                        MESSAGE_KEY_INVALID_REQUEST, null, 
                        "Request contains invalid characters", locale);
                
                ErrorResponse errorResponse = new ErrorResponse(
                        ERROR_INVALID_REQUEST, localizedMessage, null);
                
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                
                try {
                    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
                    return exchange.getResponse().writeWith(
                            Mono.just(exchange.getResponse().bufferFactory().wrap(
                                    jsonResponse.getBytes(StandardCharsets.UTF_8)
                            ))
                    );
                } catch (JsonProcessingException e) {
                    log.error("Error serializing error response", e);
                    return exchange.getResponse().setComplete();
                }
            }
            return chain.filter(exchange);
        } catch (Exception e) {
            log.error("Error in InvalidRequestFilter", e);
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
    }

    private Locale getLocaleFromRequest(ServerWebExchange exchange) {
        String acceptLanguage = exchange.getRequest().getHeaders().getFirst("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.isEmpty()) {
            try {
                String[] parts = acceptLanguage.split(",");
                if (parts.length > 0) {
                    String langTag = parts[0].trim();
                    return Locale.forLanguageTag(langTag.replace("_", "-"));
                }
            } catch (Exception e) {
                log.debug("Error parsing Accept-Language header: {}", e.getMessage());
            }
        }
        return Locale.getDefault();
    }


    private boolean containsInvalidCharacters(String str, String allowedChars) {
        if (str == null) {
            return false;
        }
        
        if (ALLOWED_CHARS_QUERY.equals(allowedChars)) {
            return str.chars().anyMatch(c -> !isAllowedQueryCharacter(c, allowedChars));
        }
        return str.chars().anyMatch(c -> !isAllowedCharacter(c, allowedChars));
    }

    private boolean isAllowedCharacter(int c, String allowedChars) {
        if (c >= 32 && c <= 126) {
            return allowedChars.indexOf(c) >= 0;
        }
        return c == '\t' || c == '\n' || c == '\r';
    }

    private boolean isAllowedQueryCharacter(int c, String allowedChars) {
        
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            return true;
        }
        
        if (c >= 32 && c <= 126) {
            return allowedChars.indexOf(c) >= 0;
        }
        
        return Character.isLetterOrDigit(c) 
                || Character.getType(c) == Character.CONNECTOR_PUNCTUATION
                || Character.getType(c) == Character.DASH_PUNCTUATION
                || Character.getType(c) == Character.START_PUNCTUATION
                || Character.getType(c) == Character.END_PUNCTUATION
                || Character.getType(c) == Character.INITIAL_QUOTE_PUNCTUATION
                || Character.getType(c) == Character.FINAL_QUOTE_PUNCTUATION
                || Character.getType(c) == Character.OTHER_PUNCTUATION;
    }
}
