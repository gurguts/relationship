package org.example.purchaseservice.exceptions;

import lombok.NonNull;

public class PurchaseNotFoundException extends RuntimeException {
    
    public PurchaseNotFoundException(@NonNull String message) {
        super(message);
    }

}
