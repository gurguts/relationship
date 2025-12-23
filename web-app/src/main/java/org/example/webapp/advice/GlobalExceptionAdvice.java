package org.example.webapp.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.webapp.exceptions.WebAppException;
import org.example.webapp.models.dto.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
            String errorMessage = messageSource.getMessage(Objects.requireNonNull(error.getDefaultMessage()),
                    null, error.getDefaultMessage(), locale);
            errors.put(fieldName, errorMessage);
        });
        log.info("Validation errors: {}", errors);
        return new ErrorResponse(
                "VALIDATION_ERROR",
                messageSource.getMessage("validation.error", null, "Validation errors", locale),
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
    @ExceptionHandler(WebAppException.class)
    public ErrorResponse handleWebAppException(WebAppException ex, Locale locale) {
        log.warn("WebApp error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        // Try to get localized message from messages.properties
        String messageKey = String.format("webapp.error.%s", ex.getErrorCode().toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        // If no localized message found, use the English message from exception
        String message = (localizedMessage != null && !localizedMessage.equals(messageKey))
                ? localizedMessage 
                : ex.getMessage();
        
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex, Locale locale) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ErrorResponse(
                "INVALID_ARGUMENT",
                messageSource.getMessage("webapp.error.INVALID_ARGUMENT", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UsernameNotFoundException.class)
    public ErrorResponse handleUsernameNotFoundException(UsernameNotFoundException ex, Locale locale) {
        log.warn("Username not found: {}", ex.getMessage());
        return new ErrorResponse(
                "USER_NOT_FOUND",
                messageSource.getMessage("webapp.error.USER_NOT_FOUND", null, ex.getMessage(), locale),
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