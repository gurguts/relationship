package org.example.userservice.repositories;

import org.example.userservice.models.branch.Branch;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends CrudRepository<Branch, Long> {
    Optional<Branch> findByName(String name);
    
    List<Branch> findAllByOrderByNameAsc();
}

