package org.example.userservice.restControllers.branch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.mappers.BranchMapper;
import org.example.userservice.models.branch.Branch;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.models.dto.branch.BranchCreateDTO;
import org.example.userservice.models.dto.branch.BranchDTO;
import org.example.userservice.services.impl.IBranchPermissionService;
import org.example.userservice.services.impl.IBranchService;
import org.example.userservice.utils.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Validated
public class BranchController {
    private final IBranchService branchService;
    private final IBranchPermissionService branchPermissionService;
    private final BranchMapper branchMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<BranchDTO>> getAllBranches() {
        Long currentUserId = getCurrentUserIdOrThrow();
        
        List<Branch> branches = branchService.getBranchesAccessibleToUser(currentUserId);
        Map<Long, BranchPermission> permissionMap = buildPermissionMap(currentUserId);
        
        List<BranchDTO> dtos = branches.stream()
                .map(branch -> branchMapper.branchToBranchDTO(branch, permissionMap))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchDTO>> getAllBranchesForAdmin() {
        Long currentUserId = getCurrentUserIdOrThrow();
        
        List<Branch> branches = branchService.getAllBranches();
        Map<Long, BranchPermission> permissionMap = buildPermissionMap(currentUserId);
        
        List<BranchDTO> dtos = branches.stream()
                .map(branch -> branchMapper.branchToBranchDTO(branch, permissionMap))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<BranchDTO> getBranchById(@PathVariable @Positive @NonNull Long id) {
        Long currentUserId = getCurrentUserIdOrThrow();
        
        Branch branch = branchService.getBranchById(id);

        if (!branchPermissionService.canView(currentUserId, id)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        
        Map<Long, BranchPermission> permissionMap = buildPermissionMapForSingleBranch(currentUserId, id);
        return ResponseEntity.ok(branchMapper.branchToBranchDTO(branch, permissionMap));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody @Valid @NonNull BranchCreateDTO dto) {
        Branch branch = branchMapper.branchCreateDTOToBranch(dto);
        Branch created = branchService.createBranch(branch);
        
        Map<Long, BranchPermission> permissionMap = Collections.emptyMap();
        BranchDTO branchDTO = branchMapper.branchToBranchDTO(created, permissionMap);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(branchDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(branchDTO);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<BranchDTO> updateBranch(
            @PathVariable @Positive @NonNull Long id,
            @RequestBody @Valid @NonNull BranchCreateDTO dto) {
        Long currentUserId = getCurrentUserIdOrThrow();
        
        Branch branch = branchMapper.branchCreateDTOToBranch(dto);
        Branch updated = branchService.updateBranch(id, branch);
        
        Map<Long, BranchPermission> permissionMap = buildPermissionMapForSingleBranch(currentUserId, id);
        return ResponseEntity.ok(branchMapper.branchToBranchDTO(updated, permissionMap));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteBranch(@PathVariable @Positive @NonNull Long id) {
        branchService.deleteBranch(id);
        branchPermissionService.deleteAllPermissionsByBranchId(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserIdOrThrow() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UserException("USER_NOT_FOUND", "Current user ID is null");
        }
        return currentUserId;
    }

    private Map<Long, BranchPermission> buildPermissionMap(@NonNull Long userId) {
        List<BranchPermission> userPermissions = branchPermissionService.getPermissionsByUserId(userId);
        return userPermissions.stream()
                .collect(Collectors.toMap(
                        BranchPermission::getBranchId,
                        p -> p,
                        (existing, _) -> existing
                ));
    }

    private Map<Long, BranchPermission> buildPermissionMapForSingleBranch(@NonNull Long userId, @NonNull Long branchId) {
        Optional<BranchPermission> permission = branchPermissionService.getPermission(userId, branchId);
        return permission.map(p -> Map.of(branchId, p))
                .orElse(Collections.emptyMap());
    }
}

