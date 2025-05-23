package org.example.authservice.exceptions;

public class UserExceptionNotFound extends RuntimeException {
    public UserExceptionNotFound(String message) {
        super(message);
    }
}
