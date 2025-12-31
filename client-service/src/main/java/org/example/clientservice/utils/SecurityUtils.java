package org.example.clientservice.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtils {
    private static final String AUTHORITY_SYSTEM_ADMIN = "system:admin";
    private static final String AUTHORITY_ADMINISTRATION_VIEW = "administration:view";
    
    private SecurityUtils() {
    }
    
    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            return null;
        }
        
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        
        if (details instanceof Number) {
            return ((Number) details).longValue();
        }
        
        log.warn("Authentication details is not a Long or Number, got: {}", details != null ? details.getClass() : "null");
        return null;
    }
    
    public static boolean isAdmin() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> AUTHORITY_SYSTEM_ADMIN.equals(auth) || AUTHORITY_ADMINISTRATION_VIEW.equals(auth));
    }
    
    public static boolean hasAuthority(@NonNull String authority) {
        if (authority.isBlank()) {
            log.warn("Authority is null or blank");
            return false;
        }
        
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
    
    private static Authentication getAuthentication() {
        try {
            return SecurityContextHolder.getContext().getAuthentication();
        } catch (Exception e) {
            log.warn("Error getting authentication from SecurityContext: {}", e.getMessage());
            return null;
        }
    }
}
