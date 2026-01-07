package org.example.userservice.exceptions.account;

import lombok.NonNull;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(@NonNull String message) {
        super(message);
    }
}

