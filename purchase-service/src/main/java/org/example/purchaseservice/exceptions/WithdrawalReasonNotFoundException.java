package org.example.purchaseservice.exceptions;

import lombok.NonNull;

public class WithdrawalReasonNotFoundException extends RuntimeException {
    
    public WithdrawalReasonNotFoundException(@NonNull String message) {
        super(message);
    }

}
