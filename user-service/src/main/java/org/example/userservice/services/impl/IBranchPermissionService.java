package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.branch.BranchPermission;

import java.util.List;
import java.util.Optional;

public interface IBranchPermissionService {
    Optional<BranchPermission> getPermission(@NonNull Long userId, @NonNull Long branchId);
    
    List<BranchPermission> getPermissionsByUserId(@NonNull Long userId);
    
    List<BranchPermission> getPermissionsByBranchId(@NonNull Long branchId);
    
    boolean canView(@NonNull Long userId, @NonNull Long branchId);
    
    boolean canOperate(@NonNull Long userId, @NonNull Long branchId);
    
    BranchPermission createOrUpdatePermission(@NonNull Long userId, @NonNull Long branchId, 
                                               Boolean canView, Boolean canOperate);
    
    void deletePermission(@NonNull Long userId, @NonNull Long branchId);
    
    void deleteAllPermissionsByBranchId(@NonNull Long branchId);
}
