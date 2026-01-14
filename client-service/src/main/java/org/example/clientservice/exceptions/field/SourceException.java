package org.example.clientservice.exceptions.field;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class SourceException extends RuntimeException {

    private final String errorCode;

    public SourceException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SourceException(@NonNull String errorCode, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}
