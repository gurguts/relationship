package org.example.sourceservice.exceptions;

import lombok.Getter;

@Getter
public class SourceException extends RuntimeException {
    private final String errorCode;
    public SourceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SourceException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
