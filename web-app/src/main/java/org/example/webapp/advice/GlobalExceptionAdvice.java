package org.example.webapp.advice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.webapp.models.dto.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionAdvice {
    private final MessageSource messageSource;

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