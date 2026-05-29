package org.example.apigateway.services;

import lombok.NonNull;
import org.example.apigateway.models.dto.UserDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class HomeRedirectService {

    private static final String DEFAULT_HOME = "/clients";

    private static final Map<String, String> ROLE_HOME_PATHS = Map.of(
            "ADMIN", "/administration",
            "MANAGER", "/clients",
            "LEADER", "/clients",
            "DRIVER", "/routes",
            "ACCOUNTANT", "/finance",
            "STOREKEEPER", "/stock",
            "DECLARANT", "/declarant"
    );

    @NonNull
    public String resolveHomePath(@NonNull UserDTO user) {
        if (user.role() != null && !user.role().isBlank()) {
            return ROLE_HOME_PATHS.getOrDefault(user.role().toUpperCase(), DEFAULT_HOME);
        }
        return resolveHomePathByAuthorities(user.authorities());
    }

    @NonNull
    private String resolveHomePathByAuthorities(@NonNull List<String> authorities) {
        if (hasAuthority(authorities, "administration:view")) {
            return "/administration";
        }
        if (hasAuthority(authorities, "declarant:view")) {
            return "/declarant";
        }
        if (hasAuthority(authorities, "warehouse:view")) {
            return "/stock";
        }
        if (hasAuthority(authorities, "finance:view")) {
            return "/finance";
        }
        if (hasAuthority(authorities, "client:view")) {
            if (hasOnlyClientView(authorities)) {
                return "/routes";
            }
            return "/clients";
        }
        return DEFAULT_HOME;
    }

    private boolean hasOnlyClientView(@NonNull List<String> authorities) {
        return !authorities.isEmpty()
                && authorities.stream().allMatch("client:view"::equals);
    }

    private boolean hasAuthority(@NonNull List<String> authorities, @NonNull String authority) {
        return authorities.contains(authority);
    }
}
