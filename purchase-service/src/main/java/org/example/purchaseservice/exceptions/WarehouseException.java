package org.example.purchaseservice.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class WarehouseException extends RuntimeException {

    private final String errorCode;

    public WarehouseException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
