package org.example.businessservice.advices;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.businessservice.exceptions.BusinessException;
import org.example.businessservice.exceptions.BusinessNotFoundException;
import org.example.businessservice.models.dto.ErrorResponse;
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
            String errorMessage = messageSource.getMessage(Objects.requireNonNull(error.getDefaultMessage()), null, error.getDefaultMessage(), locale);
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
            String errorMessage = messageSource.getMessage(violation.getMessage(), null, violation.getMessage(), locale);
            errors.put(fieldName, errorMessage);
        });
        log.info("Constraint Violation Errors: {}", errors);
        return new ErrorResponse(
                "VALIDATION_ERROR",
                messageSource.getMessage("validation.error", null, "Constraint Violation Errors", locale),
                errors
        );
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, Locale locale) {
        log.warn("Access Denied: {}", ex.getMessage());
        return new ErrorResponse(
                "ACCESS_DENIED",
                messageSource.getMessage("access.denied", null, "You do not have permission to perform this action.", locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public ErrorResponse handleBusinessException(BusinessException ex, Locale locale) {
        log.warn("Business error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        String message = messageSource.getMessage(
                "business.error." + ex.getErrorCode().toUpperCase(),
                null,
                ex.getMessage(),
                locale
        );
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
