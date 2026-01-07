package org.example.userservice.security;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.user.Permission;
import org.example.userservice.models.user.Status;
import org.example.userservice.models.user.User;
import org.example.userservice.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String login) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> {
                    log.warn("User not found with login: {}", login);
                    return new UsernameNotFoundException(String.format("User with login '%s' not found", login));
                });

        if (user.getStatus() != Status.ACTIVE) {
            log.warn("Attempt to authenticate inactive user with login: {} (status: {})", login, user.getStatus());
            throw new UsernameNotFoundException(String.format("User with login '%s' is not active", login));
        }

        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.error("User with login {} has no password", login);
            throw new UsernameNotFoundException(String.format("User with login '%s' has no password", login));
        }

        Set<GrantedAuthority> authorities = user.getPermissions() != null
                ? user.getPermissions().stream()
                        .map(Permission::getPermission)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet())
                : Collections.emptySet();

        log.debug("User {} loaded successfully with {} permissions", login, authorities.size());

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                authorities
        );
    }
}
