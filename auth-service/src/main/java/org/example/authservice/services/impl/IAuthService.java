package org.example.authservice.services.impl;

import java.util.Map;

public interface IAuthService {
    Map<String, Object> authenticate(final String login, final String password);
}
