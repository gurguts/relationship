package org.example.userservice.restControllers.branch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.models.dto.branch.BranchPermissionCreateDTO;
import org.example.userservice.models.dto.branch.BranchPermissionDTO;
import org.example.userservice.services.branch.BranchPermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/branch-permissions")
@RequiredArgsConstructor
public class BranchPermissionController {
    private final BranchPermissionService branchPermissionService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchPermissionDTO>> getPermissionsByUser(@PathVariable Long userId) {
        List<BranchPermission> permissions = branchPermissionService.getPermissionsByUserId(userId);
        List<BranchPermissionDTO> dtos = permissions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchPermissionDTO>> getPermissionsByBranch(@PathVariable Long branchId) {
        List<BranchPermission> permissions = branchPermissionService.getPermissionsByBranchId(branchId);
        List<BranchPermissionDTO> dtos = permissions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('administration:edit')")
    public ResponseEntity<BranchPermissionDTO> createOrUpdatePermission(@RequestBody BranchPermissionCreateDTO dto) {
        BranchPermission permission = branchPermissionService.createOrUpdatePermission(
                dto.getUserId(),
                dto.getBranchId(),
                dto.getCanView(),
                dto.getCanOperate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(permission));
    }

    @DeleteMapping("/user/{userId}/branch/{branchId}")
    @PreAuthorize("hasAuthority('administration:edit')")
    public ResponseEntity<Void> deletePermission(@PathVariable Long userId, @PathVariable Long branchId) {
        branchPermissionService.deletePermission(userId, branchId);
        return ResponseEntity.noContent().build();
    }

    private BranchPermissionDTO mapToDTO(BranchPermission permission) {
        return new BranchPermissionDTO(
                permission.getId(),
                permission.getUserId(),
                permission.getBranchId(),
                permission.getCanView(),
                permission.getCanOperate(),
                permission.getCreatedAt(),
                permission.getUpdatedAt()
        );
    }
}


