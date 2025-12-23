package org.example.webapp.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.webapp.exceptions.WebAppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@Getter
@Component
public class SecretKeyConfig {

    @Value("${jwt.secret}")
    private String secretKey;

    @PostConstruct
    public void init() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(secretKey.getBytes());
            SecretKeySpec secretKeySpec = new SecretKeySpec(hash, "HmacSHA256");
            secretKey = Base64.getEncoder().encodeToString(secretKeySpec.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to initialize secret key", e);
            throw new WebAppException("SECRET_KEY_ERROR", "Failed to initialize secret key: " + e.getMessage());
        }
    }

}