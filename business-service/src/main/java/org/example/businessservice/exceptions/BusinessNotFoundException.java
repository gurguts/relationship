package org.example.businessservice.exceptions;

public class BusinessNotFoundException extends RuntimeException{
    public BusinessNotFoundException(String message) {
        super(message);
    }
}
