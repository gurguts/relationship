package org.example.userservice.restControllers.branch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.mappers.BranchPermissionMapper;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.models.dto.branch.BranchPermissionCreateDTO;
import org.example.userservice.models.dto.branch.BranchPermissionDTO;
import org.example.userservice.services.impl.IBranchPermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/branch-permissions")
@RequiredArgsConstructor
@Validated
public class BranchPermissionController {
    private final IBranchPermissionService branchPermissionService;
    private final BranchPermissionMapper branchPermissionMapper;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchPermissionDTO>> getPermissionsByUser(
            @PathVariable @Positive @NonNull Long userId) {
        List<BranchPermission> permissions = branchPermissionService.getPermissionsByUserId(userId);
        List<BranchPermissionDTO> dtos = permissions.stream()
                .map(branchPermissionMapper::branchPermissionToBranchPermissionDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchPermissionDTO>> getPermissionsByBranch(
            @PathVariable @Positive @NonNull Long branchId) {
        List<BranchPermission> permissions = branchPermissionService.getPermissionsByBranchId(branchId);
        List<BranchPermissionDTO> dtos = permissions.stream()
                .map(branchPermissionMapper::branchPermissionToBranchPermissionDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('administration:edit')")
    public ResponseEntity<BranchPermissionDTO> createOrUpdatePermission(
            @RequestBody @Valid @NonNull BranchPermissionCreateDTO dto) {
        BranchPermission permission = branchPermissionService.createOrUpdatePermission(
                dto.getUserId(),
                dto.getBranchId(),
                dto.getCanView(),
                dto.getCanOperate()
        );
        BranchPermissionDTO permissionDTO = branchPermissionMapper.branchPermissionToBranchPermissionDTO(permission);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/user/{userId}/branch/{branchId}")
                .buildAndExpand(permissionDTO.getUserId(), permissionDTO.getBranchId())
                .toUri();
        return ResponseEntity.status(CREATED).location(location).body(permissionDTO);
    }

    @DeleteMapping("/user/{userId}/branch/{branchId}")
    @PreAuthorize("hasAuthority('administration:edit')")
    public ResponseEntity<Void> deletePermission(
            @PathVariable @Positive @NonNull Long userId,
            @PathVariable @Positive @NonNull Long branchId) {
        branchPermissionService.deletePermission(userId, branchId);
        return ResponseEntity.noContent().build();
    }
}
