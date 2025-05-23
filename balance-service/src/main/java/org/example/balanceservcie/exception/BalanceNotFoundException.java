package org.example.balanceservcie.exception;

public class BalanceNotFoundException extends RuntimeException {
    public BalanceNotFoundException(String message) {
        super(message);
    }
}
