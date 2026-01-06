package org.example.purchaseservice.services.user;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.UserClient;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserClient userClient;
    
    @Cacheable(value = "userFullNames", key = "#login", unless = "#result == null")
    public String getUserFullNameFromLogin(@NonNull String login) {
        log.debug("Getting user full name from login: login={}", login);
        try {
            String fullName = userClient.getUserFullNameFromLogin(login).getBody();
            if (fullName == null || fullName.trim().isEmpty()) {
                log.warn("User full name not found for login: {}", login);
                return null;
            }
            String trimmedName = fullName.trim();
            log.debug("User full name found: login={}, fullName={}", login, trimmedName);
            return trimmedName;
        } catch (Exception e) {
            log.warn("Failed to get user full name for login: {}, error: {}", login, e.getMessage());
            return null;
        }
    }
    
    public List<UserDTO> getAllUsers() {
        log.debug("Getting all users");
        try {
            List<UserDTO> users = userClient.getAllUsers().getBody();
            if (users == null) {
                log.warn("No users found");
                return List.of();
            }
            log.debug("Found {} users", users.size());
            return users;
        } catch (Exception e) {
            log.error("Failed to get all users: error={}", e.getMessage(), e);
            return List.of();
        }
    }
    
    @CacheEvict(value = "userFullNames", key = "#login")
    public void evictUserFullNameCache(@NonNull String login) {
        log.debug("Evicting user full name cache: login={}", login);
    }
    
    @CacheEvict(value = "userFullNames", allEntries = true)
    public void evictAllUserFullNamesCache() {
        log.debug("Evicting all user full names cache");
    }
}

