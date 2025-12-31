package org.example.clientservice.exceptions.client;

import lombok.NonNull;

public class ClientNotFoundException extends RuntimeException {
    
    public ClientNotFoundException(@NonNull String message) {
        super(message);
    }

}
