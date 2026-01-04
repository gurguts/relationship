package org.example.purchaseservice.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.NonNull;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    public ErrorResponse handleValidationExceptions(@NonNull MethodArgumentNotValidException ex, @NonNull Locale locale) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String defaultMessage = Objects.toString(error.getDefaultMessage(), "");
            String errorMessage = MessageLocalizationHelper.getLocalizedMessage(
                    messageSource, defaultMessage, defaultMessage, locale);
            errors.put(fieldName, errorMessage);
        });
        logRequestContext("Validation errors: {}", errors);
        return new ErrorResponse(
                ErrorConstants.ERROR_VALIDATION,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_VALIDATION,
                        ErrorConstants.DEFAULT_MESSAGE_VALIDATION, locale),
                errors
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolation(@NonNull ConstraintViolationException ex, @NonNull Locale locale) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String violationMessage = Objects.toString(violation.getMessage(), "");
            String errorMessage = MessageLocalizationHelper.getLocalizedMessage(
                    messageSource, violationMessage, violationMessage, locale);
            errors.put(fieldName, errorMessage);
        });
        logRequestContext("Constraint Violation Errors: {}", errors);
        return new ErrorResponse(
                ErrorConstants.ERROR_VALIDATION,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_VALIDATION,
                        ErrorConstants.DEFAULT_MESSAGE_CONSTRAINT_VIOLATION, locale),
                errors
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDeniedException(@NonNull AccessDeniedException ex, @NonNull Locale locale) {
        logRequestContext("Access Denied: {}", Objects.toString(ex.getMessage(), ""));
        return new ErrorResponse(
                ErrorConstants.ERROR_ACCESS_DENIED,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_ACCESS_DENIED,
                        ErrorConstants.DEFAULT_MESSAGE_ACCESS_DENIED, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PurchaseException.class)
    public ErrorResponse handlePurchaseException(@NonNull PurchaseException ex, @NonNull Locale locale) {
        String errorCode = ex.getErrorCode();
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Purchase error: code={}, message={}", errorCode, exceptionMessage);
        
        String messageKey = String.format(ErrorConstants.MESSAGE_KEY_PREFIX_PURCHASE_ERROR, errorCode.toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        boolean hasLocalizedMessage = MessageLocalizationHelper.isValidLocalizedMessage(localizedMessage, messageKey);
        
        String finalMessage = hasLocalizedMessage ? localizedMessage : exceptionMessage;
        Map<String, String> details = buildDetailsIfComplex(exceptionMessage);
        
        if (details != null && !hasLocalizedMessage) {
            finalMessage = MessageLocalizationHelper.extractFirstLine(exceptionMessage);
        }
        
        return new ErrorResponse(errorCode, finalMessage, details);
    }

    private Map<String, String> buildDetailsIfComplex(String exceptionMessage) {
        if (!MessageLocalizationHelper.isComplexMessage(exceptionMessage)) {
            return null;
        }
        
        Map<String, String> details = new HashMap<>();
        details.put(ErrorConstants.DETAILS_KEY_ERROR, exceptionMessage);
        return details;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PurchaseNotFoundException.class)
    public ErrorResponse handlePurchaseNotFoundException(@NonNull PurchaseNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Purchase not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_PURCHASE_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_PURCHASE_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ProductException.class)
    public ErrorResponse handleProductException(@NonNull ProductException ex, @NonNull Locale locale) {
        String errorCode = ex.getErrorCode();
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Product error: code={}, message={}", errorCode, exceptionMessage);
        String message = MessageLocalizationHelper.getLocalizedErrorCodeMessage(
                messageSource, errorCode,
                ErrorConstants.MESSAGE_KEY_PREFIX_PRODUCT_ERROR,
                exceptionMessage, locale);
        return new ErrorResponse(errorCode, message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProductNotFoundException.class)
    public ErrorResponse handleProductNotFoundException(@NonNull ProductNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Product not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_PRODUCT_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_PRODUCT_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WarehouseException.class)
    public ErrorResponse handleWarehouseException(@NonNull WarehouseException ex, @NonNull Locale locale) {
        String errorCode = ex.getErrorCode();
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Warehouse error: code={}, message={}", errorCode, exceptionMessage);
        String message = MessageLocalizationHelper.getLocalizedErrorCodeMessage(
                messageSource, errorCode,
                ErrorConstants.MESSAGE_KEY_PREFIX_WAREHOUSE_ERROR,
                exceptionMessage, locale);
        return new ErrorResponse(errorCode, message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(WarehouseNotFoundException.class)
    public ErrorResponse handleWarehouseNotFoundException(@NonNull WarehouseNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Warehouse not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_WAREHOUSE_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_WAREHOUSE_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(JsonProcessingException.class)
    public ErrorResponse handleJsonProcessingException(@NonNull JsonProcessingException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Invalid JSON format: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_INVALID_JSON,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_JSON_ERROR,
                        ErrorConstants.DEFAULT_MESSAGE_INVALID_JSON, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(@NonNull Exception ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContextWithException(exceptionMessage, ex);
        return new ErrorResponse(
                ErrorConstants.ERROR_SERVER_ERROR,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_SERVER_ERROR,
                        ErrorConstants.DEFAULT_MESSAGE_SERVER_ERROR, locale),
                null
        );
    }

    private void logRequestContext(String message, Object... args) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String path = Objects.toString(request.getRequestURI(), "unknown");
            String method = request.getMethod() != null ? request.getMethod() : "unknown";
            String remoteAddress = Objects.toString(request.getRemoteAddr(), "unknown");
            log.warn("{} Path: {}, Method: {}, Remote: {}", 
                    String.format(message.replace("{}", "%s"), args), path, method, remoteAddress);
        } else {
            log.warn(message, args);
        }
    }

    private void logRequestContextWithException(String exceptionMessage, Throwable ex) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String path = Objects.toString(request.getRequestURI(), "unknown");
            String method = request.getMethod() != null ? request.getMethod() : "unknown";
            String remoteAddress = Objects.toString(request.getRemoteAddr(), "unknown");
            log.error("{}: {} Path: {}, Method: {}, Remote: {}",
                    "An unexpected error occurred", exceptionMessage, path, method, remoteAddress, ex);
        } else {
            log.error("{}: {}", "An unexpected error occurred", exceptionMessage, ex);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}