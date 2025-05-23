package org.example.saleservice.exceptions;

import lombok.Getter;

@Getter
public class SaleException extends RuntimeException {
    private final String errorCode;

    public SaleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SaleException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
