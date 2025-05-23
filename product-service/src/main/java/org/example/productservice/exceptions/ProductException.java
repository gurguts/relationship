package org.example.productservice.exceptions;

import lombok.Getter;

@Getter
public class ProductException extends RuntimeException {
    private final String errorCode;
    public ProductException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProductException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
