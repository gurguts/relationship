package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.account.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AccountRepository extends CrudRepository<Account, Long> {
    @NonNull
    List<Account> findByUserId(@NonNull Long userId);
    
    @NonNull
    List<Account> findByBranchId(@NonNull Long branchId);

    @NonNull
    List<Account> findAllByOrderByNameAsc();

}

