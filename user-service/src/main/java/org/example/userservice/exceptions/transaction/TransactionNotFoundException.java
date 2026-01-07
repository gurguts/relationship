package org.example.userservice.exceptions.transaction;

import lombok.NonNull;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(@NonNull String message) {
        super(message);
    }
}
