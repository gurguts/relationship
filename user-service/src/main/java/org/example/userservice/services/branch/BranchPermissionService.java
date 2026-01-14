package org.example.userservice.services.branch;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.branch.BranchPermission;
import org.example.userservice.repositories.BranchPermissionRepository;
import org.example.userservice.services.impl.IBranchPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchPermissionService implements IBranchPermissionService {
    private static final boolean DEFAULT_CAN_VIEW = false;
    private static final boolean DEFAULT_CAN_OPERATE = false;

    private final BranchPermissionRepository branchPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchPermission> getPermission(@NonNull Long userId, @NonNull Long branchId) {
        return branchPermissionRepository.findByUserIdAndBranchId(userId, branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchPermission> getPermissionsByUserId(@NonNull Long userId) {
        return branchPermissionRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchPermission> getPermissionsByBranchId(@NonNull Long branchId) {
        return branchPermissionRepository.findByBranchId(branchId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canView(@NonNull Long userId, @NonNull Long branchId) {
        Optional<BranchPermission> permission = getPermission(userId, branchId);
        return permission.map(BranchPermission::getCanView)
                .orElse(DEFAULT_CAN_VIEW);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canOperate(@NonNull Long userId, @NonNull Long branchId) {
        Optional<BranchPermission> permission = getPermission(userId, branchId);
        return permission.map(BranchPermission::getCanOperate)
                .orElse(DEFAULT_CAN_OPERATE);
    }

    @Override
    @Transactional
    public BranchPermission createOrUpdatePermission(@NonNull Long userId, @NonNull Long branchId, 
                                                     Boolean canView, Boolean canOperate) {
        log.info("Creating or updating branch permission: userId={}, branchId={}, canView={}, canOperate={}", 
                userId, branchId, canView, canOperate);
        BranchPermission permission = findOrCreatePermission(userId, branchId);
        updatePermissionFlags(permission, canView, canOperate);
        
        BranchPermission saved = branchPermissionRepository.save(permission);
        log.info("Branch permission created or updated: id={}", saved.getId());
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

    @Override
    @Transactional
    public void deletePermission(@NonNull Long userId, @NonNull Long branchId) {
        log.info("Deleting branch permission: userId={}, branchId={}", userId, branchId);
        branchPermissionRepository.deleteByUserIdAndBranchId(userId, branchId);
        log.info("Branch permission deleted: userId={}, branchId={}", userId, branchId);
    }

    @Override
    @Transactional
    public void deleteAllPermissionsByBranchId(@NonNull Long branchId) {
        log.info("Deleting all permissions for branch: branchId={}", branchId);
        List<BranchPermission> permissions = branchPermissionRepository.findByBranchId(branchId);
        branchPermissionRepository.deleteAll(permissions);
        log.info("Deleted {} permissions for branch: branchId={}", permissions.size(), branchId);
    }
}
