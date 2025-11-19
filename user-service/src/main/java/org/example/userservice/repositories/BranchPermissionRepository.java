package org.example.userservice.repositories;

import org.example.userservice.models.branch.BranchPermission;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BranchPermissionRepository extends CrudRepository<BranchPermission, Long> {
    Optional<BranchPermission> findByUserIdAndBranchId(Long userId, Long branchId);
    
    List<BranchPermission> findByUserId(Long userId);
    
    List<BranchPermission> findByBranchId(Long branchId);
    
    void deleteByUserIdAndBranchId(Long userId, Long branchId);
}

