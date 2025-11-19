package org.example.userservice.services.branch;

import lombok.RequiredArgsConstructor;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.repositories.BranchPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BranchPermissionService {
    private final BranchPermissionRepository branchPermissionRepository;

    public Optional<BranchPermission> getPermission(Long userId, Long branchId) {
        return branchPermissionRepository.findByUserIdAndBranchId(userId, branchId);
    }

    public List<BranchPermission> getPermissionsByUserId(Long userId) {
        return branchPermissionRepository.findByUserId(userId);
    }

    public List<BranchPermission> getPermissionsByBranchId(Long branchId) {
        return branchPermissionRepository.findByBranchId(branchId);
    }

    public boolean canView(Long userId, Long branchId) {
        return getPermission(userId, branchId)
                .map(BranchPermission::getCanView)
                .orElse(false);
    }

    public boolean canOperate(Long userId, Long branchId) {
        return getPermission(userId, branchId)
                .map(BranchPermission::getCanOperate)
                .orElse(false);
    }

    @Transactional
    public BranchPermission createOrUpdatePermission(Long userId, Long branchId, Boolean canView, Boolean canOperate) {
        Optional<BranchPermission> existing = branchPermissionRepository.findByUserIdAndBranchId(userId, branchId);
        
        BranchPermission permission;
        if (existing.isPresent()) {
            permission = existing.get();
        } else {
            permission = new BranchPermission();
            permission.setUserId(userId);
            permission.setBranchId(branchId);
        }
        
        permission.setCanView(canView != null ? canView : false);
        permission.setCanOperate(canOperate != null ? canOperate : false);
        
        return branchPermissionRepository.save(permission);
    }

    @Transactional
    public void deletePermission(Long userId, Long branchId) {
        branchPermissionRepository.deleteByUserIdAndBranchId(userId, branchId);
    }

    @Transactional
    public void deleteAllPermissionsByBranchId(Long branchId) {
        branchPermissionRepository.findByBranchId(branchId)
                .forEach(permission -> branchPermissionRepository.delete(permission));
    }

    @Transactional
    public void deleteAllPermissionsByUserId(Long userId) {
        branchPermissionRepository.findByUserId(userId)
                .forEach(permission -> branchPermissionRepository.delete(permission));
    }
}


