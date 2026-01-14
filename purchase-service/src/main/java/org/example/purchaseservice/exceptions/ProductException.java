package org.example.purchaseservice.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ProductException extends RuntimeException {

    private final String errorCode;

    public ProductException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
