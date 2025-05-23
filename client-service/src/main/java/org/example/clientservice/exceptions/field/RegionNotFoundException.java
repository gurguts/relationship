package org.example.clientservice.exceptions.field;

public class RegionNotFoundException extends RuntimeException {
    public RegionNotFoundException(String message) {
        super(message);
    }
}
