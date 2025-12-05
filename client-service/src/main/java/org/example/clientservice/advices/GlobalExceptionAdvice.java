package org.example.clientservice.advices;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.exceptions.client.ClientNotFoundException;
import org.example.clientservice.exceptions.field.*;
import org.example.clientservice.models.dto.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionAdvice {
    private final MessageSource messageSource;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex, Locale locale) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = messageSource.getMessage(
                    Objects.requireNonNull(error.getDefaultMessage()), null, error.getDefaultMessage(), locale);
            errors.put(fieldName, errorMessage);
        });
        log.info("Validation errors: {}", errors);
        return new ErrorResponse(
                "VALIDATION_ERROR",
                messageSource.getMessage("validation.error", null, "Validation errors", locale),
                errors
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex, Locale locale) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = messageSource.getMessage(
                    violation.getMessage(), null, violation.getMessage(), locale);
            errors.put(fieldName, errorMessage);
        });
        log.info("Constraint Violation Errors: {}", errors);
        return new ErrorResponse(
                "VALIDATION_ERROR",
                messageSource.getMessage("validation.error", null,
                        "Constraint Violation Errors", locale),
                errors
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, Locale locale) {
        log.warn("Access Denied: {}", ex.getMessage());
        return new ErrorResponse(
                "ACCESS_DENIED",
                messageSource.getMessage("access.denied", null,
                        "You do not have permission to perform this action.", locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ClientException.class)
    public ErrorResponse handleClientException(ClientException ex, Locale locale) {
        log.warn("Client error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String localizedMessage = messageSource.getMessage(
                String.format("client.error.%s", ex.getErrorCode().toUpperCase()), null, null, locale);
        
        // Если локализованное сообщение не найдено или это ошибка импорта с деталями, используем оригинальное сообщение
        String message = (localizedMessage != null && !localizedMessage.equals(String.format("client.error.%s", ex.getErrorCode().toUpperCase()))) 
                ? localizedMessage 
                : ex.getMessage();
        
        // Если сообщение содержит переносы строк или многострочное, передаем его в details
        Map<String, String> details = null;
        if (ex.getMessage() != null && (ex.getMessage().contains("\n") || ex.getMessage().length() > 100)) {
            details = new HashMap<>();
            details.put("error", ex.getMessage());
            // Если локализованное сообщение найдено, используем его как основное сообщение
            if (localizedMessage != null && !localizedMessage.equals(String.format("client.error.%s", ex.getErrorCode().toUpperCase()))) {
                message = localizedMessage;
            } else {
                // Иначе используем первую строку как основное сообщение
                String[] lines = ex.getMessage().split("\n");
                message = lines.length > 0 ? lines[0] : ex.getMessage();
            }
        }
        
        return new ErrorResponse(ex.getErrorCode(), message, details);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ClientNotFoundException.class)
    public ErrorResponse handleClientNotFoundException(ClientNotFoundException ex, Locale locale) {
        log.warn("Client not found: {}", ex.getMessage());
        return new ErrorResponse(
                "CLIENT_NOT_FOUND",
                messageSource.getMessage("client.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public ErrorResponse handleBusinessException(BusinessException ex, Locale locale) {
        log.warn("Business error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String message = messageSource.getMessage(
                String.format("business.error.%s", ex.getErrorCode().toUpperCase()), null, ex.getMessage(), locale);
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BusinessNotFoundException.class)
    public ErrorResponse handleBusinessNotFoundException(BusinessNotFoundException ex, Locale locale) {
        log.warn("Business not found: {}", ex.getMessage());
        return new ErrorResponse(
                "BUSINESS_NOT_FOUND",
                messageSource.getMessage("business.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RegionException.class)
    public ErrorResponse handleRegionException(RegionException ex, Locale locale) {
        log.warn("Region error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String message = messageSource.getMessage(
                String.format("region.error.%s", ex.getErrorCode().toUpperCase()), null, ex.getMessage(), locale);
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RegionNotFoundException.class)
    public ErrorResponse handleRegionNotFoundException(RegionNotFoundException ex, Locale locale) {
        log.warn("Region not found: {}", ex.getMessage());
        return new ErrorResponse(
                "REGION_NOT_FOUND",
                messageSource.getMessage("region.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RouteException.class)
    public ErrorResponse handleRouteException(RouteException ex, Locale locale) {
        log.warn("Route error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String message = messageSource.getMessage(
                String.format("route.error.%s", ex.getErrorCode().toUpperCase()), null, ex.getMessage(), locale);
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(RouteNotFoundException.class)
    public ErrorResponse handleRouteNotFoundException(RouteNotFoundException ex, Locale locale) {
        log.warn("Route not found: {}", ex.getMessage());
        return new ErrorResponse(
                "ROUTE_NOT_FOUND",
                messageSource.getMessage("route.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SourceException.class)
    public ErrorResponse handleSourceException(SourceException ex, Locale locale) {
        log.warn("Source error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String message = messageSource.getMessage(
                String.format("source.error.%s", ex.getErrorCode().toUpperCase()), null, ex.getMessage(), locale);
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(SourceNotFoundException.class)
    public ErrorResponse handleSourceNotFoundException(SourceNotFoundException ex, Locale locale) {
        log.warn("Source not found: {}", ex.getMessage());
        return new ErrorResponse(
                "SOURCE_NOT_FOUND",
                messageSource.getMessage("source.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(StatusClientException.class)
    public ErrorResponse handleStatusClientException(StatusClientException ex, Locale locale) {
        log.warn("Status Client error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String message = messageSource.getMessage(
                String.format("statusClient.error.%s", ex.getErrorCode().toUpperCase()), null, ex.getMessage(),
                locale);
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(StatusClientNotFoundException.class)
    public ErrorResponse handleStatusClientNotFoundException(StatusClientNotFoundException ex, Locale locale) {
        log.warn("Status client not found: {}", ex.getMessage());
        return new ErrorResponse(
                "STATUSCLIENT_NOT_FOUND",
                messageSource.getMessage("client.notfound", null, ex.getMessage(), locale),
                null
        );
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(JsonProcessingException.class)
    public ErrorResponse handleJsonProcessingException(JsonProcessingException ex, Locale locale) {
        log.warn("Invalid JSON format: {}", ex.getMessage());
        return new ErrorResponse(
                "INVALID_JSON",
                messageSource.getMessage("json.error", null, "Invalid JSON format", locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(Exception ex, Locale locale) {
        log.error("An unexpected error occurred", ex);
        return new ErrorResponse(
                "SERVER_ERROR",
                messageSource.getMessage("server.error", null, "Internal server error", locale),
                null
        );
    }
}