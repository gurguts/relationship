package org.example.statusclientservice.exceptions;

public class StatusClientNotFoundException extends RuntimeException{
    public StatusClientNotFoundException(String message) {
        super(message);
    }
}
