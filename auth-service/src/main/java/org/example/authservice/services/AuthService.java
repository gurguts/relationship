package org.example.authservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.clients.UserServiceClient;
import org.example.authservice.exceptions.UserExceptionNotFound;
import org.example.authservice.models.dto.UserAuthDTO;
import org.example.authservice.security.JwtTokenProvider;
import org.example.authservice.services.impl.IAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final UserServiceClient userServiceClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${auth.secret.internal}")
    private String secret;

    @Override
    public Map<String, Object> authenticate(String login, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password));

        log.info("User {} authenticated", login);

        UserAuthDTO user = userServiceClient.getUserAuthByLogin(login, secret)
                .orElseThrow(() -> new UserExceptionNotFound(String.format("User with login %s not found", login)));

        String token = jwtTokenProvider.createToken(user.getId(), user.getLogin(), user.getAuthorities());

        return packageResponse(token, user);
    }

    private Map<String, Object> packageResponse(String token, UserAuthDTO user) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("login", user.getLogin());
        response.put("userId", user.getId());
        response.put("fullName", user.getFullName());
        response.put("balance", user.getBalance());
        response.put("authorities", user.getAuthorities());
        response.put("role", user.translateRole());
        response.put("expiration", jwtTokenProvider.getValidityMilliseconds());
        return response;
    }
}
