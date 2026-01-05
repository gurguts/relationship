package org.example.purchaseservice.security;

final class SecurityConstants {

    static final String BEARER_PREFIX = "Bearer ";
    static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();
    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String CLAIM_USER_ID = "userId";
    static final String CLAIM_AUTHORITIES = "authorities";

    private SecurityConstants() {
    }
}

