package org.example.clientservice.exceptions.client;

import lombok.Getter;

@Getter
public class ClientException extends RuntimeException {
    private final String errorCode;

    public ClientException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClientException(String message) {
        super(message);
        errorCode = "DEFAULT";
    }
}