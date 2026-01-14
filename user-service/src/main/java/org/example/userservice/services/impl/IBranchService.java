package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.branch.Branch;

import java.util.List;

public interface IBranchService {
    List<Branch> getAllBranches();
    
    List<Branch> getBranchesAccessibleToUser(@NonNull Long userId);
    
    Branch getBranchById(@NonNull Long id);
    
    Branch createBranch(@NonNull Branch branch);
    
    Branch updateBranch(@NonNull Long id, @NonNull Branch updatedBranch);
    
    void deleteBranch(@NonNull Long id);
}
