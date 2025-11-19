package org.example.userservice.repositories;

import org.example.userservice.models.account.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends CrudRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    
    List<Account> findByBranchId(Long branchId);
    
    List<Account> findByUserIdIsNull();
    
    List<Account> findAllByOrderByNameAsc();
    
    Optional<Account> findByIdAndUserId(Long id, Long userId);
}

