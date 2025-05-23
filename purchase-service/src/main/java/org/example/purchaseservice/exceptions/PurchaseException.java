package org.example.purchaseservice.exceptions;

import lombok.Getter;

@Getter
public class PurchaseException extends RuntimeException {
    private final String errorCode;

    public PurchaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PurchaseException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
