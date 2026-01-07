package org.example.userservice.exceptions.transaction;

import lombok.NonNull;

public class TransactionCategoryNotFoundException extends RuntimeException {

    public TransactionCategoryNotFoundException(@NonNull String message) {
        super(message);
    }
}

