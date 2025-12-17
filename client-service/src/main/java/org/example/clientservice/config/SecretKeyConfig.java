package org.example.clientservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Configuration
public class SecretKeyConfig {

    @Value("${jwt.secret}")
    private String rawSecretKey;

    @Bean
    public String secretKey() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(rawSecretKey.getBytes());
            SecretKeySpec secretKeySpec = new SecretKeySpec(hash, "HmacSHA256");
            return Base64.getEncoder().encodeToString(secretKeySpec.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize secret key", e);
        }
    }

}