package org.example.clientservice.exceptions.field;

public class BusinessNotFoundException extends RuntimeException {
    public BusinessNotFoundException(String message) {
        super(message);
    }
}
