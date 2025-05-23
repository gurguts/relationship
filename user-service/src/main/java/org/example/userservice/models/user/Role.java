package org.example.userservice.models.user;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN(),

    MANAGER(),

    STOREKEEPER(),

    LEADER(),

    ACCOUNTANT(),

    DRIVER()
}