package org.example.apigateway.advice;

final class ErrorConstants {
    
    static final String ERROR_BAD_REQUEST = "BAD_REQUEST";
    static final String ERROR_NOT_FOUND = "NOT_FOUND";
    static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    static final String ERROR_UNAUTHORIZED = "UNAUTHORIZED";
    static final String ERROR_SERVER_ERROR = "SERVER_ERROR";
    
    static final String MESSAGE_RESOURCE_NOT_FOUND = "Resource not found";
    static final String MESSAGE_ACCESS_DENIED = "You do not have permission to perform this action.";
    static final String MESSAGE_AUTHENTICATION_FAILED = "Authentication failed";
    static final String MESSAGE_INTERNAL_SERVER_ERROR = "Internal server error";
    
    private ErrorConstants() {
    }
}

