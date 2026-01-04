package org.example.purchaseservice.advice;

final class ErrorConstants {

    static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    static final String ERROR_ACCESS_DENIED = "ACCESS_DENIED";
    static final String ERROR_PURCHASE_NOT_FOUND = "PURCHASE_NOT_FOUND";
    static final String ERROR_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    static final String ERROR_WAREHOUSE_NOT_FOUND = "WAREHOUSE_NOT_FOUND";
    static final String ERROR_INVALID_JSON = "INVALID_JSON";
    static final String ERROR_SERVER_ERROR = "SERVER_ERROR";

    static final String MESSAGE_KEY_VALIDATION = "validation.error";
    static final String MESSAGE_KEY_ACCESS_DENIED = "access.denied";
    static final String MESSAGE_KEY_PURCHASE_NOT_FOUND = "purchase.notfound";
    static final String MESSAGE_KEY_PRODUCT_NOT_FOUND = "product.notfound";
    static final String MESSAGE_KEY_WAREHOUSE_NOT_FOUND = "warehouse.notfound";
    static final String MESSAGE_KEY_JSON_ERROR = "json.error";
    static final String MESSAGE_KEY_SERVER_ERROR = "server.error";

    static final String MESSAGE_KEY_PREFIX_PURCHASE_ERROR = "purchase.error.%s";
    static final String MESSAGE_KEY_PREFIX_PRODUCT_ERROR = "product.error.%s";
    static final String MESSAGE_KEY_PREFIX_WAREHOUSE_ERROR = "warehouse.error.%s";

    static final String DEFAULT_MESSAGE_VALIDATION = "Validation errors";
    static final String DEFAULT_MESSAGE_CONSTRAINT_VIOLATION = "Constraint Violation Errors";
    static final String DEFAULT_MESSAGE_ACCESS_DENIED = "You do not have permission to perform this action.";
    static final String DEFAULT_MESSAGE_INVALID_JSON = "Invalid JSON format";
    static final String DEFAULT_MESSAGE_SERVER_ERROR = "Internal server error";

    static final String DETAILS_KEY_ERROR = "error";

    static final int MAX_MESSAGE_LENGTH_FOR_DETAILS = 100;

    private ErrorConstants() {
    }
}
