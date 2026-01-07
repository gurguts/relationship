package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.branch.BranchPermission;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BranchPermissionRepository extends CrudRepository<BranchPermission, Long> {
    Optional<BranchPermission> findByUserIdAndBranchId(@NonNull Long userId, @NonNull Long branchId);
    
    @NonNull
    List<BranchPermission> findByUserId(@NonNull Long userId);
    
    @NonNull
    List<BranchPermission> findByBranchId(@NonNull Long branchId);
    
    void deleteByUserIdAndBranchId(@NonNull Long userId, @NonNull Long branchId);
}

