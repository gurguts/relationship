package org.example.userservice.exceptions.transaction;

public class TransactionCategoryNotFoundException extends RuntimeException {
    public TransactionCategoryNotFoundException(String message) {
        super(message);
    }
}

