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
        List<BranchDTO> dtos = branches.stream()
                .map(branch -> mapToDTO(branch, currentUserId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('administration:view')")
    public ResponseEntity<List<BranchDTO>> getAllBranchesForAdmin() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<Branch> branches = branchService.getAllBranches();
        List<BranchDTO> dtos = branches.stream()
                .map(branch -> mapToDTO(branch, currentUserId))
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
        
        return ResponseEntity.ok(mapToDTO(branch, currentUserId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings_finance:create')")
    public ResponseEntity<BranchDTO> createBranch(@RequestBody BranchCreateDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setDescription(dto.getDescription());
        Branch created = branchService.createBranch(branch);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(created, currentUserId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:edit')")
    public ResponseEntity<BranchDTO> updateBranch(@PathVariable Long id, @RequestBody BranchCreateDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setDescription(dto.getDescription());
        Branch updated = branchService.updateBranch(id, branch);
        return ResponseEntity.ok(mapToDTO(updated, currentUserId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);

        branchPermissionService.deleteAllPermissionsByBranchId(id);
        return ResponseEntity.noContent().build();
    }

    private BranchDTO mapToDTO(Branch branch, Long userId) {
        Boolean canView = branchPermissionService.canView(userId, branch.getId());
        Boolean canOperate = branchPermissionService.canOperate(userId, branch.getId());
        
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

