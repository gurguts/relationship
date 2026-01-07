package org.example.purchaseservice.utils;

import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    
    private static final String AUTHORITY_SYSTEM_ADMIN = "system:admin";
    private static final String AUTHORITY_ADMINISTRATION_VIEW = "administration:view";
    
    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            return null;
        }
        return authentication.getDetails() instanceof Long ?
                (Long) authentication.getDetails() : null;
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
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> authority.equals(auth));
    }

    public static String getCurrentUserLogin() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }
}

