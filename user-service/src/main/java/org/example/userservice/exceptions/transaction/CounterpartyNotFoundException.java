package org.example.userservice.exceptions.transaction;

import lombok.NonNull;

public class CounterpartyNotFoundException extends RuntimeException {

    public CounterpartyNotFoundException(@NonNull String message) {
        super(message);
    }
}

