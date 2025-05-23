package org.example.userservice.exceptions.balance;

public class BalanceNotFoundException extends RuntimeException {
    public BalanceNotFoundException(String message) {
        super(message);
    }
}
