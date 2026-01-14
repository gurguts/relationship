package org.example.containerservice.exceptions;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class ContainerException extends RuntimeException {

    private final String errorCode;

    public ContainerException(@NonNull String errorCode, @NonNull String message) {
        super(message);
        this.errorCode = errorCode;
    }

}