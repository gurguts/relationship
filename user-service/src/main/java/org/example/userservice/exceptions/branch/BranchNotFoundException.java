package org.example.userservice.exceptions.branch;

import lombok.NonNull;

public class BranchNotFoundException extends RuntimeException {

    public BranchNotFoundException(@NonNull String message) {
        super(message);
    }
}

