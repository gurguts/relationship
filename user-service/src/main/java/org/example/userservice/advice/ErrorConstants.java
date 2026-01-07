package org.example.userservice.advice;

final class ErrorConstants {

    static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
    static final String ERROR_TRANSACTION_NOT_FOUND = "TRANSACTION_NOT_FOUND";
    static final String ERROR_TRANSACTION_CATEGORY_NOT_FOUND = "TRANSACTION_CATEGORY_NOT_FOUND";
    static final String ERROR_ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    static final String ERROR_BRANCH_NOT_FOUND = "BRANCH_NOT_FOUND";
    static final String ERROR_INVALID_JSON = "INVALID_JSON";
    static final String ERROR_AUTH_ERROR = "AUTH_ERROR";
    static final String ERROR_SERVER_ERROR = "SERVER_ERROR";

    static final String MESSAGE_KEY_VALIDATION = "validation.error";
    static final String MESSAGE_KEY_ACCESS_DENIED = "access.denied";
    static final String MESSAGE_KEY_USER_NOT_FOUND = "user.notfound";
    static final String MESSAGE_KEY_TRANSACTION_NOT_FOUND = "transaction.notfound";
    static final String MESSAGE_KEY_TRANSACTION_CATEGORY_NOT_FOUND = "transaction.category.notfound";
    static final String MESSAGE_KEY_ACCOUNT_NOT_FOUND = "account.notfound";
    static final String MESSAGE_KEY_BRANCH_NOT_FOUND = "branch.notfound";
    static final String MESSAGE_KEY_JSON_ERROR = "json.error";
    static final String MESSAGE_KEY_AUTH_ERROR = "auth.error";
    static final String MESSAGE_KEY_SERVER_ERROR = "server.error";

    static final String MESSAGE_KEY_PREFIX_USER_ERROR = "user.error.%s";
    static final String MESSAGE_KEY_PREFIX_TRANSACTION_ERROR = "transaction.error.%s";
    static final String MESSAGE_KEY_PREFIX_ACCOUNT_ERROR = "account.error.%s";

    static final String DEFAULT_MESSAGE_VALIDATION = "Validation errors";
    static final String DEFAULT_MESSAGE_CONSTRAINT_VIOLATION = "Constraint Violation Errors";
    static final String DEFAULT_MESSAGE_ACCESS_DENIED = "You do not have permission to perform this action.";
    static final String DEFAULT_MESSAGE_INVALID_JSON = "Invalid JSON format";
    static final String DEFAULT_MESSAGE_AUTH_ERROR = "Invalid login credentials";
    static final String DEFAULT_MESSAGE_SERVER_ERROR = "Internal server error";

    static final String DETAILS_KEY_ERROR = "error";

    static final int MAX_MESSAGE_LENGTH_FOR_DETAILS = 100;

    private ErrorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}

