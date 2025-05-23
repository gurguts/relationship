package org.example.regionservice.exceptions;

public class RegionNotFoundException extends RuntimeException {
    public RegionNotFoundException(String message) {
        super(message);
    }
}
