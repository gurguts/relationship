package org.example.containerservice.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ContainerException extends RuntimeException {
    
    private static final String DEFAULT_ERROR_CODE = "DEFAULT";
    
    private final String errorCode;

    public ContainerException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ContainerException(@NonNull String errorCode, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ContainerException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public ContainerException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}