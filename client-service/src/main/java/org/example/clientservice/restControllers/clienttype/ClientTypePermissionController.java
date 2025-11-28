package org.example.clientservice.restControllers.clienttype;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionCreateDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionDTO;
import org.example.clientservice.models.dto.clienttype.ClientTypePermissionUpdateDTO;
import org.example.clientservice.services.impl.IClientTypePermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/client-type")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ClientTypePermissionController {
    private final IClientTypePermissionService permissionService;

    @PreAuthorize("hasAuthority('administration:view')")
    @PostMapping("/{clientTypeId}/permission")
    public ResponseEntity<ClientTypePermissionDTO> createPermission(
            @PathVariable Long clientTypeId,
            @RequestBody @Valid ClientTypePermissionCreateDTO dto) {
        ClientTypePermission permission = permissionService.createPermission(clientTypeId, dto);
        ClientTypePermissionDTO response = permissionToDTO(permission);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/../permission/{userId}")
                .buildAndExpand(dto.getUserId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PreAuthorize("hasAuthority('administration:view')")
    @GetMapping("/{clientTypeId}/permission")
    public ResponseEntity<List<ClientTypePermissionDTO>> getPermissionsByClientTypeId(@PathVariable Long clientTypeId) {
        List<ClientTypePermission> permissions = permissionService.getPermissionsByClientTypeId(clientTypeId);
        List<ClientTypePermissionDTO> response = permissions.stream()
                .map(this::permissionToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:view')")
    @PutMapping("/{clientTypeId}/permission/{userId}")
    public ResponseEntity<ClientTypePermissionDTO> updatePermission(
            @PathVariable Long clientTypeId,
            @PathVariable Long userId,
            @RequestBody @Valid ClientTypePermissionUpdateDTO dto) {
        ClientTypePermission permission = permissionService.updatePermission(clientTypeId, userId, dto);
        ClientTypePermissionDTO response = permissionToDTO(permission);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:view')")
    @DeleteMapping("/{clientTypeId}/permission/{userId}")
    public ResponseEntity<Void> deletePermission(
            @PathVariable Long clientTypeId,
            @PathVariable Long userId) {
        permissionService.deletePermission(clientTypeId, userId);
        return ResponseEntity.noContent().build();
    }

    private ClientTypePermissionDTO permissionToDTO(ClientTypePermission permission) {
        ClientTypePermissionDTO dto = new ClientTypePermissionDTO();
        dto.setId(permission.getId());
        dto.setUserId(permission.getUserId());
        dto.setClientTypeId(permission.getClientType().getId());
        dto.setClientTypeName(permission.getClientType().getName());
        dto.setCanView(permission.getCanView());
        dto.setCanCreate(permission.getCanCreate());
        dto.setCanEdit(permission.getCanEdit());
        dto.setCanDelete(permission.getCanDelete());
        dto.setCreatedAt(permission.getCreatedAt());
        return dto;
    }
}

