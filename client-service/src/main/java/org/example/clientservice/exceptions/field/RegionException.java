package org.example.clientservice.exceptions.field;

import lombok.Getter;

@Getter
public class RegionException extends RuntimeException {
    private final String errorCode;

    public RegionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RegionException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
