package org.example.userservice.services.impl;

import lombok.NonNull;

import java.util.Map;

public interface IAuthService {
    @NonNull Map<String, Object> authenticate(@NonNull String login, @NonNull String password);
}
