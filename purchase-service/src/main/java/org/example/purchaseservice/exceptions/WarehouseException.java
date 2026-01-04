package org.example.purchaseservice.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class WarehouseException extends RuntimeException {
    
    private static final String DEFAULT_ERROR_CODE = "DEFAULT";
    
    private final String errorCode;

    public WarehouseException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WarehouseException(@NonNull String errorCode, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public WarehouseException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}
