package org.example.clientservice.exceptions.field;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class SourceException extends RuntimeException {
    
    private static final String DEFAULT_ERROR_CODE = "DEFAULT";
    
    private final String errorCode;

    public SourceException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SourceException(@NonNull String errorCode, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SourceException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public SourceException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}
