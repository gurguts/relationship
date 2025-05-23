package org.example.businessservice.exceptions;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    private final String errorCode;

    public BusinessException(String message){
        super(message);
        this.errorCode = "DEFAULT";
    }
}
