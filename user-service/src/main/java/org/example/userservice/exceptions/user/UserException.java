package org.example.userservice.exceptions.user;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class UserException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "DEFAULT";

    private final String errorCode;

    public UserException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserException(@NonNull String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }
}
