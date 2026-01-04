package org.example.containerservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtils {

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

    private static Authentication getAuthentication() {
        try {
            return SecurityContextHolder.getContext().getAuthentication();
        } catch (Exception e) {
            log.warn("Error getting authentication from SecurityContext: {}", e.getMessage());
            return null;
        }
    }
}
