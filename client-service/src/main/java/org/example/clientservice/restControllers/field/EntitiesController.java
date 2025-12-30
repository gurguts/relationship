package org.example.clientservice.restControllers.field;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.ProductClient;
import org.example.clientservice.clients.UserClient;
import org.example.clientservice.mappers.field.SourceMapper;
import org.example.clientservice.models.dto.fields.EntitiesDTO;
import org.example.clientservice.services.field.SourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Slf4j
public class EntitiesController {
    private final SourceService sourceService;
    private final UserClient userClient;
    private final ProductClient productClient;

    private final SourceMapper sourceMapper;

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping
    public ResponseEntity<EntitiesDTO> getAllEntities() {
        EntitiesDTO entities = new EntitiesDTO();
        entities.setSources(sourceService.getAllSources().stream()
                .map(sourceMapper::sourceToSourceDTO)
                .toList());
        entities.setUsers(Objects.requireNonNull(userClient.getAllUsers().getBody()).stream()
                .toList());
        entities.setProducts(Objects.requireNonNull(productClient.getAllProducts().getBody()).stream()
                .toList());

        return ResponseEntity.ok(entities);
    }
}
