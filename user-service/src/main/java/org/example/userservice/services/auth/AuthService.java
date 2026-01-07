package org.example.userservice.services.auth;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.security.JwtTokenProvider;
import org.example.userservice.services.impl.IAuthService;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private static final String ERROR_CODE_AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR";
    private static final String MESSAGES_BUNDLE = "messages";
    private static final String ROLE_KEY_PREFIX = "role.";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @NotNull
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> authenticate(@NonNull String login, @NonNull String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password));

        log.info("User {} authenticated", login);

        User user = getUserAfterAuthentication(login);
        String token = createTokenForUser(user);

        return packageResponse(token, user);
    }

    private User getUserAfterAuthentication(@NonNull String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    log.error("User not found after successful authentication: {}", login);
                    return new UserException(ERROR_CODE_AUTHENTICATION_ERROR, 
                            "User not found after successful authentication");
                });
    }

    private String createTokenForUser(@NonNull User user) {
        List<String> permissions = extractPermissions(user);
        return jwtTokenProvider.createToken(user.getId(), user.getLogin(), permissions);
    }

    private List<String> extractPermissions(@NonNull User user) {
        return user.getPermissions().stream()
                .map(Permission::getPermission)
                .toList();
    }

    private Map<String, Object> packageResponse(@NonNull String token, @NonNull User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("login", user.getLogin());
        response.put("userId", user.getId());
        response.put("fullName", user.getFullName());
        response.put("authorities", extractPermissions(user));
        response.put("role", getLocalizedRole(user));
        response.put("expiration", jwtTokenProvider.getValidityMilliseconds());
        return response;
    }

    private String getLocalizedRole(@NonNull User user) {
        ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BUNDLE);
        String roleKey = ROLE_KEY_PREFIX + user.getRole().name().toLowerCase();
        return bundle.getString(roleKey);
    }
}
