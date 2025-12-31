package org.example.clientservice.restControllers.clienttype;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.mappers.clienttype.ClientTypePermissionMapper;
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
@Validated
public class ClientTypePermissionController {
    private final IClientTypePermissionService permissionService;
    private final ClientTypePermissionMapper permissionMapper;

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping("/{clientTypeId}/permission")
    public ResponseEntity<ClientTypePermissionDTO> createPermission(
            @PathVariable @Positive Long clientTypeId,
            @RequestBody @Valid @NonNull ClientTypePermissionCreateDTO dto) {
        ClientTypePermission permission = permissionService.createPermission(clientTypeId, dto);
        ClientTypePermissionDTO response = permissionMapper.toDTO(permission);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/../permission/{userId}")
                .buildAndExpand(dto.getUserId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PreAuthorize("hasAuthority('administration:view')")
    @GetMapping("/{clientTypeId}/permission")
    public ResponseEntity<List<ClientTypePermissionDTO>> getPermissionsByClientTypeId(@PathVariable @Positive Long clientTypeId) {
        List<ClientTypePermission> permissions = permissionService.getPermissionsByClientTypeId(clientTypeId);
        List<ClientTypePermissionDTO> response = permissions.stream()
                .map(permissionMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PutMapping("/{clientTypeId}/permission/{userId}")
    public ResponseEntity<ClientTypePermissionDTO> updatePermission(
            @PathVariable @Positive Long clientTypeId,
            @PathVariable @Positive Long userId,
            @RequestBody @Valid @NonNull ClientTypePermissionUpdateDTO dto) {
        ClientTypePermission permission = permissionService.updatePermission(clientTypeId, userId, dto);
        ClientTypePermissionDTO response = permissionMapper.toDTO(permission);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @DeleteMapping("/{clientTypeId}/permission/{userId}")
    public ResponseEntity<Void> deletePermission(
            @PathVariable @Positive Long clientTypeId,
            @PathVariable @Positive Long userId) {
        permissionService.deletePermission(clientTypeId, userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('administration:view')")
    @GetMapping("/permission/user/{userId}")
    public ResponseEntity<List<ClientTypePermissionDTO>> getPermissionsByUserId(@PathVariable @Positive Long userId) {
        List<ClientTypePermission> permissions = permissionService.getPermissionsByUserId(userId);
        List<ClientTypePermissionDTO> response = permissions.stream()
                .map(permissionMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/permission/me")
    public ResponseEntity<List<ClientTypePermissionDTO>> getMyPermissions() {
        List<ClientTypePermissionDTO> response = permissionService.getMyPermissions();
        return ResponseEntity.ok(response);
    }
}

