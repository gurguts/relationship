package org.example.userservice.exceptions.account;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class AccountException extends RuntimeException {

    private final String errorCode;

    public AccountException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

}

