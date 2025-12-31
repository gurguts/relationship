package org.example.clientservice.exceptions.client;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ClientException extends RuntimeException {
    
    private static final String DEFAULT_ERROR_CODE = "DEFAULT";
    
    private final String errorCode;

    public ClientException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClientException(@NonNull String errorCode, @NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ClientException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public ClientException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}