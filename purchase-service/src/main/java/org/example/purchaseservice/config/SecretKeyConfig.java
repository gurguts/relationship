package org.example.purchaseservice.config;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Configuration
public class SecretKeyConfig {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final String ALGORITHM_SHA256 = "SHA-256";
    private static final String ALGORITHM_HMAC_SHA256 = "HmacSHA256";

    @Value("${jwt.secret}")
    private String secret;

    @Bean
    @NonNull
    public String secretKey() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret cannot be null or empty");
        }

        String trimmedSecret = secret.trim();
        if (trimmedSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("JWT secret must be at least %d characters long", MIN_SECRET_LENGTH));
        }

        try {
            MessageDigest sha256 = MessageDigest.getInstance(ALGORITHM_SHA256);
            byte[] hash = sha256.digest(trimmedSecret.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(hash, ALGORITHM_HMAC_SHA256);
            return Base64.getEncoder().encodeToString(secretKeySpec.getEncoded());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}