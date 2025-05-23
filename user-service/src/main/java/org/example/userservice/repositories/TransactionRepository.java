package org.example.userservice.repositories;

import org.example.userservice.models.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    @NotNull
    Page<Transaction> findAll(Specification<Transaction> specification, @NotNull Pageable pageable);
}
