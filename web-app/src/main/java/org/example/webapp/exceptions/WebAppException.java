package org.example.webapp.exceptions;

import lombok.Getter;

@Getter
public class WebAppException extends RuntimeException {
    private final String errorCode;

    public WebAppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WebAppException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}

