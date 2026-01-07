package org.example.userservice.config;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Getter
@Component
public class SecretKeyConfig {

    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    @Value("${jwt.secret}")
    @NonNull
    private String secret;

    @PostConstruct
    public void init() {
        validateSecret();
        try {
            MessageDigest sha256 = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] hash = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(hash, HMAC_SHA256_ALGORITHM);
            secret = Base64.getEncoder().encodeToString(secretKeySpec.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Bean
    @NonNull
    public SecretKey secretKey() {
        validateSecret();
        try {
            byte[] decodedKey = Base64.getDecoder().decode(secret);
            return Keys.hmacShaKeyFor(decodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid secret key format", e);
        }
    }

    private void validateSecret() {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret cannot be null or empty");
        }
    }
}