package org.example.purchaseservice.exceptions;

import lombok.NonNull;

public class WarehouseNotFoundException extends RuntimeException {
    
    public WarehouseNotFoundException(@NonNull String message) {
        super(message);
    }

}
