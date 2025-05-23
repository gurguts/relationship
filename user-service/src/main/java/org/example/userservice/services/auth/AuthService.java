package org.example.userservice.services.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.user.UserNotFoundException;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.User;
import org.example.userservice.repositories.UserRepository;
import org.example.userservice.security.JwtTokenProvider;
import org.example.userservice.services.impl.IAuthService;
import org.example.userservice.services.impl.IBalanceService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final IBalanceService balanceService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    public Map<String, Object> authenticate(String login, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, password));

        log.info("User {} authenticated", login);

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new UserNotFoundException(String.format("User with login %s not found", login)));

        String token = jwtTokenProvider.createToken(user.getId(), user.getLogin(),
                user.getPermissions().stream()
                        .map(Permission::getPermission)
                        .collect(Collectors.toList())
        );

        return packageResponse(token, user);
    }

    private Map<String, Object> packageResponse(String token, User user) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("login", user.getLogin());
        response.put("userId", user.getId());
        response.put("fullName", user.getFullName());
        response.put("balance", balanceService.getBalanceByUserId(user.getId()).getBalanceUAH());
        response.put("authorities",
                user.getPermissions().stream()
                        .map(Permission::getPermission)
                        .collect(Collectors.toList())
        );
        response.put("role", bundle.getString(String.format("role.%s", user.getRole().name().toLowerCase())));
        response.put("expiration", jwtTokenProvider.getValidityMilliseconds());
        return response;
    }
}
