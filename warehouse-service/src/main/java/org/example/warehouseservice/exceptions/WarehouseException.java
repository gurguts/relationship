package org.example.warehouseservice.exceptions;

import lombok.Getter;

@Getter
public class WarehouseException extends RuntimeException{
    private final String errorCode;
    public WarehouseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public WarehouseException(String message) {
        super(message);
        this.errorCode = "DEFAULT";
    }
}
