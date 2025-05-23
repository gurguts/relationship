package org.example.clientservice.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Getter
@Component
public class SecretKeyConfig {

    @Value("${jwt.secret}")
    private String secretKey;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha256.digest(secretKey.getBytes());
        SecretKeySpec secretKeySpec = new SecretKeySpec(hash, "HmacSHA256");
        secretKey = Base64.getEncoder().encodeToString(secretKeySpec.getEncoded());
    }

}