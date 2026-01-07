package org.example.userservice.services.branch;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.branch.BranchNotFoundException;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.models.branch.Branch;
import org.example.userservice.repositories.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {
    private static final String ERROR_CODE_BRANCH_ALREADY_EXISTS = "BRANCH_ALREADY_EXISTS";

    private final BranchRepository branchRepository;
    private final BranchPermissionService branchPermissionService;

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranchesAccessibleToUser(@NonNull Long userId) {
        List<Branch> allBranches = branchRepository.findAllByOrderByNameAsc();
        Set<Long> accessibleBranchIds = getAccessibleBranchIds(userId);
        
        return allBranches.stream()
                .filter(branch -> accessibleBranchIds.contains(branch.getId()))
                .toList();
    }

    private Set<Long> getAccessibleBranchIds(@NonNull Long userId) {
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(userId);
        return userPermissions.stream()
                .filter(org.example.userservice.models.branch.BranchPermission::getCanView)
                .map(org.example.userservice.models.branch.BranchPermission::getBranchId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(@NonNull Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Branch not found: {}", id);
                    return new BranchNotFoundException(
                            String.format("Branch with ID %d not found", id));
                });
    }

    @Transactional
    public Branch createBranch(@NonNull Branch branch) {
        validateBranchNameUniqueness(branch.getName());
        
        Branch saved = branchRepository.save(branch);
        log.debug("Created branch {} with name {}", saved.getId(), saved.getName());
        return saved;
    }

    private void validateBranchNameUniqueness(@NonNull String name) {
        if (branchRepository.findByName(name).isPresent()) {
            log.warn("Attempt to create branch with existing name: {}", name);
            throw new UserException(ERROR_CODE_BRANCH_ALREADY_EXISTS, 
                    String.format("Branch with name '%s' already exists", name));
        }
    }

    @Transactional
    public Branch updateBranch(@NonNull Long id, @NonNull Branch updatedBranch) {
        Branch branch = getBranchById(id);
        updateBranchFields(branch, updatedBranch);
        
        Branch saved = branchRepository.save(branch);
        log.debug("Updated branch {}", id);
        return saved;
    }

    private void updateBranchFields(@NonNull Branch branch, @NonNull Branch updatedBranch) {
        branch.setName(updatedBranch.getName());
        branch.setDescription(updatedBranch.getDescription());
    }

    @Transactional
    public void deleteBranch(@NonNull Long id) {
        Branch branch = getBranchById(id);
        branchRepository.delete(branch);
        log.debug("Deleted branch {}", id);
    }
}
