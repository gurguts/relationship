package org.example.clientservice.restControllers.field;

import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.dto.fields.EntitiesDTO;
import org.example.clientservice.services.impl.IEntitiesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Validated
public class EntitiesController {
    private final IEntitiesService entitiesService;

    @PreAuthorize("hasAuthority('client:view')")
    @GetMapping
    public ResponseEntity<EntitiesDTO> getAllEntities() {
        EntitiesDTO entities = entitiesService.getAllEntities();
        return ResponseEntity.ok(entities);
    }
}
