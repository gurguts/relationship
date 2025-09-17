package org.example.purchaseservice.exceptions;

public class WithdrawalReasonNotFoundException extends RuntimeException {
    public WithdrawalReasonNotFoundException(String message) {
        super(message);
    }
}
