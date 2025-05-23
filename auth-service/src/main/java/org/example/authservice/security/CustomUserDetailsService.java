package org.example.authservice.security;

import lombok.RequiredArgsConstructor;
import org.example.authservice.clients.UserServiceClient;
import org.example.authservice.models.UserDet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserServiceClient userServiceClient;

    @Value("${auth.secret.internal}")
    private String secret;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        UserDet user = userServiceClient.getUserDetByLogin(login, secret)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User with login %s not found", login)));
        Set<GrantedAuthority> authorities = user.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                authorities
        );
    }
}

