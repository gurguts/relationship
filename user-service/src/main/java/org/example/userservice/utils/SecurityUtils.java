package org.example.userservice.utils;

import org.example.userservice.exceptions.user.UserException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof Long userId) {
            return userId;
        }
        throw new UserException("AUTHENTICATION_REQUIRED", "User is not authenticated or userId is not available");
    }

}


