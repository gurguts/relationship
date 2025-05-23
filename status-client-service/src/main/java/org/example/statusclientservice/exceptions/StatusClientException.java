package org.example.statusclientservice.exceptions;

import lombok.Getter;

@Getter
public class StatusClientException extends RuntimeException {
    private final String errorCode;

    public StatusClientException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StatusClientException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
