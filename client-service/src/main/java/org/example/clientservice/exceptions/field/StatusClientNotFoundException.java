package org.example.clientservice.exceptions.field;

public class StatusClientNotFoundException extends RuntimeException {
    public StatusClientNotFoundException(String message) {
        super(message);
    }
}
