package org.example.userservice.restControllers.branch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.branch.Branch;
import org.example.userservice.models.dto.branch.BranchCreateDTO;
import org.example.userservice.models.dto.branch.BranchDTO;
import org.example.userservice.services.branch.BranchPermissionService;
import org.example.userservice.services.branch.BranchService;
import org.example.userservice.utils.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {
    private final BranchService branchService;
    private final BranchPermissionService branchPermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<BranchDTO>> getAllBranches() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<Branch> branches = branchService.getBranchesAccessibleToUser(currentUserId);
        
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(currentUserId);
        java.util.Map<Long, org.example.userservice.models.branch.BranchPermission> permissionMap = 
                userPermissions.stream()
                        .collect(Collectors.toMap(
                                org.example.userservice.models.branch.BranchPermission::getBranchId,
                                p -> p,
                                (existing, replacement) -> existing
                        ));
        
        List<BranchDTO> dtos = branches.stream()
                .map(branch -> mapToDTO(branch, permissionMap))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchDTO>> getAllBranchesForAdmin() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<Branch> branches = branchService.getAllBranches();
        
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(currentUserId);
        java.util.Map<Long, org.example.userservice.models.branch.BranchPermission> permissionMap = 
                userPermissions.stream()
                        .collect(Collectors.toMap(
                                org.example.userservice.models.branch.BranchPermission::getBranchId,
                                p -> p,
                                (existing, replacement) -> existing
                        ));
        
        List<BranchDTO> dtos = branches.stream()
                .map(branch -> mapToDTO(branch, permissionMap))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Branch branch = branchService.getBranchById(id);

        if (!branchPermissionService.canView(currentUserId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        java.util.Optional<org.example.userservice.models.branch.BranchPermission> permission = 
                branchPermissionService.getPermission(currentUserId, id);
        java.util.Map<Long, org.example.userservice.models.branch.BranchPermission> permissionMap = 
                permission.map(p -> java.util.Map.of(id, p))
                        .orElse(java.util.Collections.emptyMap());
        
        return ResponseEntity.ok(mapToDTO(branch, permissionMap));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody BranchCreateDTO dto) {
        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setDescription(dto.getDescription());
        Branch created = branchService.createBranch(branch);
        
        java.util.Map<Long, org.example.userservice.models.branch.BranchPermission> permissionMap = 
                java.util.Collections.emptyMap();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(created, permissionMap));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @RequestBody BranchCreateDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setDescription(dto.getDescription());
        Branch updated = branchService.updateBranch(id, branch);
        
        java.util.Optional<org.example.userservice.models.branch.BranchPermission> permission = 
                branchPermissionService.getPermission(currentUserId, id);
        java.util.Map<Long, org.example.userservice.models.branch.BranchPermission> permissionMap = 
                permission.map(p -> java.util.Map.of(id, p))
                        .orElse(java.util.Collections.emptyMap());
        
        return ResponseEntity.ok(mapToDTO(updated, permissionMap));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);

        branchPermissionService.deleteAllPermissionsByBranchId(id);
        return ResponseEntity.noContent().build();
    }

    private BranchDTO mapToDTO(Branch branch, java.util.Map<Long, org.example.userservice.models.branch.BranchPermission> permissionMap) {
        org.example.userservice.models.branch.BranchPermission permission = permissionMap.get(branch.getId());
        Boolean canView = permission != null && permission.getCanView();
        Boolean canOperate = permission != null && permission.getCanOperate();
        
        return new BranchDTO(
                branch.getId(),
                branch.getName(),
                branch.getDescription(),
                branch.getCreatedAt(),
                branch.getUpdatedAt(),
                canView,
                canOperate
        );
    }
}

