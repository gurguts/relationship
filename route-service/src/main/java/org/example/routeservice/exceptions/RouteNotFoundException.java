package org.example.routeservice.exceptions;

public class RouteNotFoundException extends RuntimeException{
    public RouteNotFoundException(String message) {
        super(message);
    }
}
