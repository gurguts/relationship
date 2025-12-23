package org.example.userservice.services.branch;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.branch.BranchNotFoundException;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.models.branch.Branch;
import org.example.userservice.repositories.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {
    private final BranchRepository branchRepository;
    private final BranchPermissionService branchPermissionService;

    @Transactional(readOnly = true)
    public List<Branch> getAllBranches() {
        return branchRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranchesAccessibleToUser(Long userId) {
        List<Branch> allBranches = branchRepository.findAllByOrderByNameAsc();
        
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(userId);
        java.util.Set<Long> accessibleBranchIds = userPermissions.stream()
                .filter(org.example.userservice.models.branch.BranchPermission::getCanView)
                .map(org.example.userservice.models.branch.BranchPermission::getBranchId)
                .collect(Collectors.toSet());
        
        return allBranches.stream()
                .filter(branch -> accessibleBranchIds.contains(branch.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Branch> getBranchesOperableByUser(Long userId) {
        List<Branch> allBranches = branchRepository.findAllByOrderByNameAsc();
        
        List<org.example.userservice.models.branch.BranchPermission> userPermissions = 
                branchPermissionService.getPermissionsByUserId(userId);
        java.util.Set<Long> operableBranchIds = userPermissions.stream()
                .filter(org.example.userservice.models.branch.BranchPermission::getCanOperate)
                .map(org.example.userservice.models.branch.BranchPermission::getBranchId)
                .collect(Collectors.toSet());
        
        return allBranches.stream()
                .filter(branch -> operableBranchIds.contains(branch.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Branch getBranchById(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new BranchNotFoundException(
                        String.format("Branch with ID %d not found", id)));
    }

    @Transactional
    public Branch createBranch(Branch branch) {
        if (branchRepository.findByName(branch.getName()).isPresent()) {
            throw new UserException("BRANCH_ALREADY_EXISTS", "Branch with name '" + branch.getName() + "' already exists");
        }
        return branchRepository.save(branch);
    }

    @Transactional
    public Branch updateBranch(Long id, Branch updatedBranch) {
        Branch branch = getBranchById(id);
        branch.setName(updatedBranch.getName());
        branch.setDescription(updatedBranch.getDescription());
        return branchRepository.save(branch);
    }

    @Transactional
    public void deleteBranch(Long id) {
        Branch branch = getBranchById(id);
        branchRepository.delete(branch);
    }
}

