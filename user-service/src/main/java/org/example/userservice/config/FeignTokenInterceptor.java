package org.example.userservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignTokenInterceptor implements RequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(@NonNull RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object credentials = authentication.getCredentials();
            if (credentials instanceof String token && !token.trim().isEmpty()) {
                template.header(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
            }
        }
    }
}