package org.example.webapp.config;

public final class SecurityConstants {

    public static final String[] PUBLIC_PATH_PATTERNS = {
            "/login",
            "/api/v1/auth/**",
            "/api/v1/user/login",
            "/favicon.ico",
            "/favicon/**",
            "/js/login.js",
            "/css/**"
    };

    private SecurityConstants() {
    }
}
