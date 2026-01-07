package org.example.userservice.exceptions.user;

import lombok.NonNull;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(@NonNull String message) {
        super(message);
    }
}
