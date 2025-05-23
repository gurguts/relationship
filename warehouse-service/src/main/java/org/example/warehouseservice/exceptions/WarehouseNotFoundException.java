package org.example.warehouseservice.exceptions;

public class WarehouseNotFoundException extends RuntimeException{
    public WarehouseNotFoundException(String message) {
        super(message);
    }
}
