package org.example.userservice.exceptions.user;

import lombok.Getter;

@Getter
public class UserException extends RuntimeException {
    private final String errorCode;

    public UserException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
