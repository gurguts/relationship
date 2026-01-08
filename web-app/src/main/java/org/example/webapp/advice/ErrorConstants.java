package org.example.webapp.advice;

final class ErrorConstants {

    static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
    static final String ERROR_INVALID_ARGUMENT = "INVALID_ARGUMENT";
    static final String ERROR_INVALID_JSON = "INVALID_JSON";
    static final String ERROR_SERVER_ERROR = "SERVER_ERROR";

    static final String MESSAGE_KEY_VALIDATION = "validation.error";
    static final String MESSAGE_KEY_ACCESS_DENIED = "access.denied";
    static final String MESSAGE_KEY_USER_NOT_FOUND = "webapp.error.USER_NOT_FOUND";
    static final String MESSAGE_KEY_INVALID_ARGUMENT = "webapp.error.INVALID_ARGUMENT";
    static final String MESSAGE_KEY_JSON_ERROR = "json.error";
    static final String MESSAGE_KEY_SERVER_ERROR = "server.error";

    static final String MESSAGE_KEY_PREFIX_WEBAPP_ERROR = "webapp.error.%s";

    static final String DEFAULT_MESSAGE_VALIDATION = "Validation errors";
    static final String DEFAULT_MESSAGE_ACCESS_DENIED = "You do not have permission to perform this action.";
    static final String DEFAULT_MESSAGE_INVALID_JSON = "Invalid JSON format";
    static final String DEFAULT_MESSAGE_SERVER_ERROR = "Internal server error";

    private ErrorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}

