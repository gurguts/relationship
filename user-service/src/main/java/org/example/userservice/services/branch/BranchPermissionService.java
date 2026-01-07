package org.example.userservice.services.branch;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.repositories.BranchPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchPermissionService {
    private static final boolean DEFAULT_CAN_VIEW = false;
    private static final boolean DEFAULT_CAN_OPERATE = false;

    private final BranchPermissionRepository branchPermissionRepository;

    @Transactional(readOnly = true)
    public Optional<BranchPermission> getPermission(@NonNull Long userId, @NonNull Long branchId) {
        return branchPermissionRepository.findByUserIdAndBranchId(userId, branchId);
    }

    @Transactional(readOnly = true)
    public List<BranchPermission> getPermissionsByUserId(@NonNull Long userId) {
        return branchPermissionRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<BranchPermission> getPermissionsByBranchId(@NonNull Long branchId) {
        return branchPermissionRepository.findByBranchId(branchId);
    }

    @Transactional(readOnly = true)
    public boolean canView(@NonNull Long userId, @NonNull Long branchId) {
        Optional<BranchPermission> permission = getPermission(userId, branchId);
        return permission.map(BranchPermission::getCanView)
                .orElse(DEFAULT_CAN_VIEW);
    }

    @Transactional(readOnly = true)
    public boolean canOperate(@NonNull Long userId, @NonNull Long branchId) {
        Optional<BranchPermission> permission = getPermission(userId, branchId);
        return permission.map(BranchPermission::getCanOperate)
                .orElse(DEFAULT_CAN_OPERATE);
    }

    @Transactional
    public BranchPermission createOrUpdatePermission(@NonNull Long userId, @NonNull Long branchId, 
                                                     Boolean canView, Boolean canOperate) {
        BranchPermission permission = findOrCreatePermission(userId, branchId);
        updatePermissionFlags(permission, canView, canOperate);
        
        BranchPermission saved = branchPermissionRepository.save(permission);
        log.debug("Created or updated permission for user {} and branch {}", userId, branchId);
        return saved;
    }

    private BranchPermission findOrCreatePermission(@NonNull Long userId, @NonNull Long branchId) {
        return branchPermissionRepository.findByUserIdAndBranchId(userId, branchId)
                .orElseGet(() -> {
                    BranchPermission newPermission = new BranchPermission();
                    newPermission.setUserId(userId);
                    newPermission.setBranchId(branchId);
                    return newPermission;
                });
    }

    private void updatePermissionFlags(@NonNull BranchPermission permission, Boolean canView, Boolean canOperate) {
        permission.setCanView(canView != null ? canView : DEFAULT_CAN_VIEW);
        permission.setCanOperate(canOperate != null ? canOperate : DEFAULT_CAN_OPERATE);
    }

    @Transactional
    public void deletePermission(@NonNull Long userId, @NonNull Long branchId) {
        branchPermissionRepository.deleteByUserIdAndBranchId(userId, branchId);
        log.debug("Deleted permission for user {} and branch {}", userId, branchId);
    }

    @Transactional
    public void deleteAllPermissionsByBranchId(@NonNull Long branchId) {
        List<BranchPermission> permissions = branchPermissionRepository.findByBranchId(branchId);
        branchPermissionRepository.deleteAll(permissions);
        log.debug("Deleted {} permissions for branch {}", permissions.size(), branchId);
    }
}
