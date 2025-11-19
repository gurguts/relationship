package org.example.userservice.exceptions.account;

public class AccountException extends RuntimeException {
    private final String field;
    
    public AccountException(String field, String message) {
        super(message);
        this.field = field;
    }
    
    public String getField() {
        return field;
    }
}

