package org.example.purchaseservice.exceptions;

import lombok.NonNull;

public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException(@NonNull String message) {
        super(message);
    }

}
