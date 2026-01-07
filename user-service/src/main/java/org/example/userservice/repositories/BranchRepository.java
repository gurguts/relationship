package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.branch.Branch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends CrudRepository<Branch, Long> {
    Optional<Branch> findByName(@NonNull String name);
    
    @NonNull
    List<Branch> findAllByOrderByNameAsc();
}

