package org.example.apigateway.advice;

import org.example.apigateway.exceptions.JwtAuthenticationException;
import org.example.apigateway.models.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.reactive.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.function.Function;

record ExceptionMapping(Class<? extends Throwable> exceptionType, 
                        Function<Throwable, ErrorResponse> responseBuilder, 
                        HttpStatus httpStatus) {
    
    static ExceptionMapping[] getMappings() {
        return new ExceptionMapping[]{
            new ExceptionMapping(IllegalArgumentException.class, 
                ex -> new ErrorResponse(ErrorConstants.ERROR_BAD_REQUEST, 
                    Objects.toString(ex.getMessage(), ErrorConstants.ERROR_BAD_REQUEST), null),
                HttpStatus.BAD_REQUEST),
            new ExceptionMapping(IllegalStateException.class,
                ex -> new ErrorResponse(ErrorConstants.ERROR_BAD_REQUEST,
                    Objects.toString(ex.getMessage(), ErrorConstants.ERROR_BAD_REQUEST), null),
                HttpStatus.BAD_REQUEST),
            new ExceptionMapping(NoResourceFoundException.class,
                ex -> new ErrorResponse(ErrorConstants.ERROR_NOT_FOUND,
                    ErrorConstants.MESSAGE_RESOURCE_NOT_FOUND, null),
                HttpStatus.NOT_FOUND),
            new ExceptionMapping(AccessDeniedException.class,
                ex -> new ErrorResponse(ErrorConstants.ERROR_ACCESS_DENIED,
                    ErrorConstants.MESSAGE_ACCESS_DENIED, null),
                HttpStatus.FORBIDDEN),
            new ExceptionMapping(AuthenticationException.class,
                ex -> new ErrorResponse(ErrorConstants.ERROR_UNAUTHORIZED,
                    Objects.toString(ex.getMessage(), ErrorConstants.MESSAGE_AUTHENTICATION_FAILED), null),
                HttpStatus.UNAUTHORIZED),
            new ExceptionMapping(JwtAuthenticationException.class,
                ex -> new ErrorResponse(ErrorConstants.ERROR_UNAUTHORIZED,
                    Objects.toString(ex.getMessage(), ErrorConstants.MESSAGE_AUTHENTICATION_FAILED), null),
                HttpStatus.UNAUTHORIZED)
        };
    }
}

