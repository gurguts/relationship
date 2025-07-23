package org.example.clientservice.exceptions.field;

import lombok.Getter;

@Getter
public class ClientProductException extends RuntimeException {
    private final String errorCode;

    public ClientProductException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClientProductException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
