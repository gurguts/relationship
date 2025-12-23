package org.example.userservice.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.account.AccountException;
import org.example.userservice.exceptions.account.AccountNotFoundException;
import org.example.userservice.exceptions.branch.BranchNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionCategoryNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.exceptions.transaction.TransactionNotFoundException;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.exceptions.user.UserNotFoundException;
import org.example.userservice.models.dto.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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
    @ExceptionHandler(UserException.class)
    public ErrorResponse handleUserException(UserException ex, Locale locale) {
        log.warn("User error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        // Try to get localized message from messages.properties
        String messageKey = String.format("user.error.%s", ex.getErrorCode().toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        // If no localized message found, use the English message from exception
        String message = (localizedMessage != null && !localizedMessage.equals(messageKey))
                ? localizedMessage 
                : ex.getMessage();
        
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ErrorResponse handleUserNotFoundException(UserNotFoundException ex, Locale locale) {
        log.warn("User not found: {}", ex.getMessage());
        return new ErrorResponse(
                "USER_NOT_FOUND",
                messageSource.getMessage("user.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TransactionException.class)
    public ErrorResponse handleTransactionException(TransactionException ex, Locale locale) {
        log.warn("Transaction error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        
        // Try to get localized message from messages.properties
        String messageKey = String.format("transaction.error.%s", ex.getErrorCode().toUpperCase());
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
    @ExceptionHandler(TransactionNotFoundException.class)
    public ErrorResponse handleTransactionNotFoundException(TransactionNotFoundException ex, Locale locale) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return new ErrorResponse(
                "TRANSACTION_NOT_FOUND",
                messageSource.getMessage("transaction.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(AccountException ex, Locale locale) {
        log.warn("Account error: field={}, message={}", ex.getField(), ex.getMessage());
        
        // Use field as error code
        String messageKey = String.format("account.error.%s", ex.getField().toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        // If no localized message found, use the English message from exception
        String message = (localizedMessage != null && !localizedMessage.equals(messageKey))
                ? localizedMessage 
                : ex.getMessage();
        
        return new ErrorResponse(ex.getField(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public ErrorResponse handleAccountNotFoundException(AccountNotFoundException ex, Locale locale) {
        log.warn("Account not found: {}", ex.getMessage());
        return new ErrorResponse(
                "ACCOUNT_NOT_FOUND",
                messageSource.getMessage("account.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TransactionCategoryNotFoundException.class)
    public ErrorResponse handleTransactionCategoryNotFoundException(TransactionCategoryNotFoundException ex, Locale locale) {
        log.warn("Transaction category not found: {}", ex.getMessage());
        return new ErrorResponse(
                "TRANSACTION_CATEGORY_NOT_FOUND",
                messageSource.getMessage("transaction.category.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BranchNotFoundException.class)
    public ErrorResponse handleBranchNotFoundException(BranchNotFoundException ex, Locale locale) {
        log.warn("Branch not found: {}", ex.getMessage());
        return new ErrorResponse(
                "BRANCH_NOT_FOUND",
                messageSource.getMessage("branch.notfound", null, ex.getMessage(), locale),
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

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex, Locale locale) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return new ErrorResponse(
                "AUTH_ERROR",
                messageSource.getMessage("auth.error", null, "Invalid login credentials", locale),
                null);
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