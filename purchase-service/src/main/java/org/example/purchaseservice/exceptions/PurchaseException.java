package org.example.purchaseservice.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class PurchaseException extends RuntimeException {
    
    private static final String DEFAULT_ERROR_CODE = "DEFAULT";
    
    private final String errorCode;

    public PurchaseException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PurchaseException(@NonNull String errorCode, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public PurchaseException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}
