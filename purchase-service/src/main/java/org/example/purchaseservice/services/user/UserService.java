package org.example.purchaseservice.services.user;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.clients.UserClient;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserClient userClient;
    
    public String getUserFullNameFromLogin(@NonNull String login) {
        if (login.trim().isEmpty()) {
            return null;
        }
        
        try {
            String fullName = userClient.getUserFullNameFromLogin(login).getBody();
            if (fullName == null || fullName.trim().isEmpty()) {
                return null;
            }
            return fullName.trim();
        } catch (FeignException e) {
            log.error("Feign error getting user full name: login={}, status={}, error={}", 
                    login, e.status(), e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error getting user full name: login={}, error={}", 
                    login, e.getMessage(), e);
            return null;
        }
    }
    
    public List<UserDTO> getAllUsers() {
        try {
            List<UserDTO> users = userClient.getAllUsers().getBody();
            return users != null ? users : Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error getting all users: status={}, error={}", 
                    e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error getting all users: error={}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}

