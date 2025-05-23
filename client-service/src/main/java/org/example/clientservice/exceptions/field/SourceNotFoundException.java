package org.example.clientservice.exceptions.field;

public class SourceNotFoundException extends RuntimeException {
    public SourceNotFoundException(String message) {
        super(message);
    }
}
