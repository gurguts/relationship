package org.example.containerservice.exceptions;

public class ContainerNotFoundException extends RuntimeException {
    public ContainerNotFoundException(String message) {
        super(message);
    }
}
