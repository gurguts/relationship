package org.example.purchaseservice.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.*;
import org.example.purchaseservice.models.dto.ErrorResponse;
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
            String errorMessage =
                    messageSource.getMessage(Objects.requireNonNull(error.getDefaultMessage()), null,
                            error.getDefaultMessage(), locale);
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
            String errorMessage = messageSource.getMessage(violation.getMessage(), null,
                    violation.getMessage(), locale);
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
    @ExceptionHandler(PurchaseException.class)
    public ErrorResponse handlePurchaseException(PurchaseException ex, Locale locale) {
        log.warn("Purchase error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        // Try to get localized message from messages.properties
        String messageKey = String.format("purchase.error.%s", ex.getErrorCode().toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        // If no localized message found, use the English message from exception
        String message = (localizedMessage != null && !localizedMessage.equals(messageKey))
                ? localizedMessage 
                : ex.getMessage();
        
        Map<String, String> details = null;
        // For complex error messages (with newlines or long text), put full message in details
        if (ex.getMessage() != null && (ex.getMessage().contains("\n") || ex.getMessage().length() > 100)) {
            details = new HashMap<>();
            details.put("error", ex.getMessage());
            // Use localized message if available, otherwise use first line of English message
            if (localizedMessage != null && !localizedMessage.equals(messageKey)) {
                message = localizedMessage;
            } else {
                String[] lines = ex.getMessage().split("\n");
                message = lines.length > 0 ? lines[0] : ex.getMessage();
            }
        }
        
        return new ErrorResponse(ex.getErrorCode(), message, details);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PurchaseNotFoundException.class)
    public ErrorResponse handlePurchaseNotFoundException(PurchaseNotFoundException ex, Locale locale) {
        log.warn("Purchase not found: {}", ex.getMessage());
        return new ErrorResponse(
                "PURCHASE_NOT_FOUND",
                messageSource.getMessage("purchase.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ProductException.class)
    public ErrorResponse handleProductException(ProductException ex, Locale locale) {
        log.warn("Product error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        // Try to get localized message from messages.properties
        String messageKey = String.format("product.error.%s", ex.getErrorCode().toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        // If no localized message found, use the English message from exception
        String message = (localizedMessage != null && !localizedMessage.equals(messageKey))
                ? localizedMessage 
                : ex.getMessage();
        
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProductNotFoundException.class)
    public ErrorResponse handleProductNotFoundException(ProductNotFoundException ex, Locale locale) {
        log.warn("Product not found: {}", ex.getMessage());
        return new ErrorResponse(
                "PRODUCT_NOT_FOUND",
                messageSource.getMessage("product.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WarehouseException.class)
    public ErrorResponse handleWarehouseException(WarehouseException ex, Locale locale) {
        log.warn("Warehouse error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        // Try to get localized message from messages.properties
        String messageKey = String.format("warehouse.error.%s", ex.getErrorCode().toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        // If no localized message found, use the English message from exception
        String message = (localizedMessage != null && !localizedMessage.equals(messageKey))
                ? localizedMessage 
                : ex.getMessage();
        
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(WarehouseNotFoundException.class)
    public ErrorResponse handleWarehouseNotFoundException(WarehouseNotFoundException ex, Locale locale) {
        log.warn("Warehouse not found: {}", ex.getMessage());
        return new ErrorResponse(
                "WAREHOUSE_NOT_FOUND",
                messageSource.getMessage("warehouse.notfound", null, ex.getMessage(), locale),
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