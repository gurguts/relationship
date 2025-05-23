package org.example.authservice.security;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Getter
@Component
public class SecretKeyConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(secret.getBytes());
        SecretKeySpec secretKeySpec = new SecretKeySpec(hash, "HmacSHA256");
        secret = Base64.getEncoder().encodeToString(secretKeySpec.getEncoded());
    }
}