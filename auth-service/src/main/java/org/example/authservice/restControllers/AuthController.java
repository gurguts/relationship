package org.example.authservice.restControllers;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.models.dto.LoginDTO;
import org.example.authservice.services.impl.IAuthService;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginDTO loginDto) {
        Map<String, Object> response = authService.authenticate(loginDto.getLogin(), loginDto.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("authToken", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
