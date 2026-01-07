package org.example.userservice.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.NonNull;
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
    @ExceptionHandler(UserException.class)
    public ErrorResponse handleUserException(@NonNull UserException ex, @NonNull Locale locale) {
        String errorCode = ex.getErrorCode();
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("User error: code={}, message={}", errorCode, exceptionMessage);
        
        String message = MessageLocalizationHelper.getLocalizedErrorCodeMessage(
                messageSource, errorCode,
                ErrorConstants.MESSAGE_KEY_PREFIX_USER_ERROR,
                exceptionMessage, locale);
        
        return new ErrorResponse(errorCode, message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ErrorResponse handleUserNotFoundException(@NonNull UserNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("User not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_USER_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_USER_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TransactionException.class)
    public ErrorResponse handleTransactionException(@NonNull TransactionException ex, @NonNull Locale locale) {
        String errorCode = ex.getErrorCode();
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Transaction error: code={}, message={}", errorCode, exceptionMessage);
        
        String messageKey = String.format(ErrorConstants.MESSAGE_KEY_PREFIX_TRANSACTION_ERROR, errorCode.toUpperCase());
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
    @ExceptionHandler(TransactionNotFoundException.class)
    public ErrorResponse handleTransactionNotFoundException(@NonNull TransactionNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Transaction not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_TRANSACTION_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_TRANSACTION_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(AccountException.class)
    public ErrorResponse handleAccountException(@NonNull AccountException ex, @NonNull Locale locale) {
        String errorCode = ex.getErrorCode();
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Account error: code={}, message={}", errorCode, exceptionMessage);
        
        String message = MessageLocalizationHelper.getLocalizedErrorCodeMessage(
                messageSource, errorCode,
                ErrorConstants.MESSAGE_KEY_PREFIX_ACCOUNT_ERROR,
                exceptionMessage, locale);
        
        return new ErrorResponse(errorCode, message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(AccountNotFoundException.class)
    public ErrorResponse handleAccountNotFoundException(@NonNull AccountNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Account not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_ACCOUNT_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_ACCOUNT_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(TransactionCategoryNotFoundException.class)
    public ErrorResponse handleTransactionCategoryNotFoundException(@NonNull TransactionCategoryNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Transaction category not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_TRANSACTION_CATEGORY_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_TRANSACTION_CATEGORY_NOT_FOUND,
                        exceptionMessage, locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BranchNotFoundException.class)
    public ErrorResponse handleBranchNotFoundException(@NonNull BranchNotFoundException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Branch not found: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_BRANCH_NOT_FOUND,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_BRANCH_NOT_FOUND,
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

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(@NonNull BadCredentialsException ex, @NonNull Locale locale) {
        String exceptionMessage = Objects.toString(ex.getMessage(), "");
        logRequestContext("Authentication failed: {}", exceptionMessage);
        return new ErrorResponse(
                ErrorConstants.ERROR_AUTH_ERROR,
                MessageLocalizationHelper.getLocalizedMessage(
                        messageSource, ErrorConstants.MESSAGE_KEY_AUTH_ERROR,
                        ErrorConstants.DEFAULT_MESSAGE_AUTH_ERROR, locale),
                null);
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