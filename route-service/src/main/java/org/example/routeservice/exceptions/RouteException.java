package org.example.routeservice.exceptions;

import lombok.Getter;

@Getter
public class RouteException extends RuntimeException {
    private final String errorCode;

    public RouteException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RouteException(String message){
        super(message);
        this.errorCode="DEFAULT";
    }
}
