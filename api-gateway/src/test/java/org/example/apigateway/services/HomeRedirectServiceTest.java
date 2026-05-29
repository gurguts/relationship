package org.example.apigateway.services;

import org.example.apigateway.models.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeRedirectServiceTest {

    private HomeRedirectService homeRedirectService;

    @BeforeEach
    void setUp() {
        homeRedirectService = new HomeRedirectService();
    }

    @Test
    void resolvesHomeByRole() {
        UserDTO user = new UserDTO("admin", List.of("administration:view"), "ADMIN");

        assertEquals("/administration", homeRedirectService.resolveHomePath(user));
    }

    @Test
    void resolvesDriverHomeByRole() {
        UserDTO user = new UserDTO("driver", List.of("client:view"), "DRIVER");

        assertEquals("/routes", homeRedirectService.resolveHomePath(user));
    }

    @Test
    void fallsBackToRoutesForDriverLikePermissionsWithoutRole() {
        UserDTO user = new UserDTO("driver", List.of("client:view"), null);

        assertEquals("/routes", homeRedirectService.resolveHomePath(user));
    }

    @Test
    void fallsBackToClientsForManagerLikePermissionsWithoutRole() {
        UserDTO user = new UserDTO("manager", List.of("client:view", "purchase:view"), null);

        assertEquals("/clients", homeRedirectService.resolveHomePath(user));
    }
}
