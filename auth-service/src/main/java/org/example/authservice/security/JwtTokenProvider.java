package org.example.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {
    private final SecretKeyConfig secretKeyConfig;

    private final JwtParser jwtParser;

    @Value("${jwt.expiration}")
    private long validityMilliseconds;


    public JwtTokenProvider(SecretKeyConfig secretKeyConfig) {
        this.secretKeyConfig = secretKeyConfig;
        this.jwtParser = Jwts.parser()
                .verifyWith(getSigningKey())
                .build();
    }

    private SecretKey getSigningKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyConfig.getSecret());
        return Keys.hmacShaKeyFor(decodedKey);
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser.parseSignedClaims(token).getPayload();
        String username = claims.getSubject();

        List<GrantedAuthority> authorities = new ArrayList<>();
        Object authoritiesClaim = claims.get("authorities");

        if (authoritiesClaim instanceof List<?>) {
            for (Object authority : (List<?>) authoritiesClaim) {
                if (authority instanceof String) {
                    authorities.add(new SimpleGrantedAuthority((String) authority));
                }
            }
        } else {
            throw new IllegalArgumentException("Authorities claim is not a list of strings");
        }

        return new UsernamePasswordAuthenticationToken(username, "", authorities);
    }

    public String createToken(Long userId, String login, List<String> authorities) {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyConfig.getSecret());
        SecretKey key = Keys.hmacShaKeyFor(decodedKey);

        Claims claims = Jwts.claims()
                .subject(login)
                .add("authorities", authorities)
                .add("userId", userId)
                .build();

        Date now = new Date();
        Date validity = new Date(now.getTime() + getValidityMilliseconds());

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public long getValidityMilliseconds (){
        return validityMilliseconds *1000;
    }
}
