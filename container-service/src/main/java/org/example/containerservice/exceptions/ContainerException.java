package org.example.containerservice.exceptions;

import lombok.Getter;

@Getter
public class ContainerException extends RuntimeException {
    private final String errorCode;

    public ContainerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ContainerException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}