package org.example.clientservice.exceptions.field;

import lombok.NonNull;

public class SourceNotFoundException extends RuntimeException {
    
    public SourceNotFoundException(@NonNull String message) {
        super(message);
    }

}
