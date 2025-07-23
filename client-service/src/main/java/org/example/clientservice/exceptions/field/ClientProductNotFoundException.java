package org.example.clientservice.exceptions.field;

public class ClientProductNotFoundException extends RuntimeException {
    public ClientProductNotFoundException(String message) {
        super(message);
    }
}
