package org.example.containerservice.exceptions;

import lombok.NonNull;

public class ContainerNotFoundException extends RuntimeException {
    
    public ContainerNotFoundException(@NonNull String message) {
        super(message);
    }

}
