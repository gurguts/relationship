package org.example.apigateway.config;

public final class SecurityConstants {

    public static final String[] PUBLIC_PATH_PATTERNS = {
            "/login",
            "/api/v1/auth/**",
            "/favicon.ico",
            "/api/v1/user/auth/**",
            "/api/v1/user/details/**",
            "/favicon/**",
            "/favicon/site.webmanifest",
            "/js/login.js",
            "/css/**"
    };

    public static final String TOKEN_ATTRIBUTE = "EXTRACTED_TOKEN";

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String AUTH_TOKEN_COOKIE = "authToken";

    private SecurityConstants() {
    }
}

