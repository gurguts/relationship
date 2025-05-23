package org.example.balanceservcie.advices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.balanceservcie.exception.BalanceException;
import org.example.balanceservcie.exception.BalanceNotFoundException;
import org.example.balanceservcie.models.dto.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Locale;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionAdvice {
    private final MessageSource messageSource;

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex, Locale locale) {
        log.warn("Access denied: {}", ex.getMessage());
        return new ErrorResponse(
                "ACCESS_DENIED",
                messageSource.getMessage("access.denied", null, "You do not have permission to perform this action.", locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BalanceException.class)
    public ErrorResponse handleBalanceException(BalanceException ex, Locale locale) {
        log.warn("Balance error: message={}", ex.getMessage());
        String message = messageSource.getMessage(
                "balance.error." + ex.getErrorCode().toUpperCase(),
                null,
                ex.getMessage(),
                locale
        );
        return new ErrorResponse(ex.getErrorCode(), message, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BalanceNotFoundException.class)
    public ErrorResponse handleBalanceNotFoundException(BalanceNotFoundException ex, Locale locale) {
        log.warn("Balance not found: {}", ex.getMessage());
        return new ErrorResponse(
                "BALANCE_NOT_FOUND",
                messageSource.getMessage("balance.notfound", null, ex.getMessage(), locale),
                null
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(Exception ex, Locale locale) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ErrorResponse(
                "SERVER_ERROR",
                messageSource.getMessage("server.error", null, "Internal server error", locale),
                null
        );
    }
}
