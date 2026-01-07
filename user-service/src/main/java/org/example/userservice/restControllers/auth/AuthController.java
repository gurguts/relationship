package org.example.userservice.restControllers.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.models.dto.user.LoginDTO;
import org.example.userservice.services.impl.IAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private static final String AUTH_TOKEN_COOKIE_NAME = "authToken";
    private static final String COOKIE_PATH = "/";
    private static final int COOKIE_MAX_AGE_DELETE = 0;

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestBody @Valid @NonNull LoginDTO loginDto) {
        Map<String, Object> response = authService.authenticate(loginDto.getLogin(), loginDto.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout(@NonNull HttpServletResponse response) {
        Cookie cookie = new Cookie(AUTH_TOKEN_COOKIE_NAME, "");
        cookie.setMaxAge(COOKIE_MAX_AGE_DELETE);
        cookie.setPath(COOKIE_PATH);
        response.addCookie(cookie);
        return ResponseEntity.noContent().build();
    }
}
