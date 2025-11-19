package org.example.userservice.repositories;

import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionCategoryRepository extends CrudRepository<TransactionCategory, Long> {
    List<TransactionCategory> findByTypeAndIsActiveTrueOrderByNameAsc(TransactionType type);
    
    List<TransactionCategory> findByTypeOrderByNameAsc(TransactionType type);
    
    Optional<TransactionCategory> findByTypeAndName(TransactionType type, String name);
    
    boolean existsByTypeAndName(TransactionType type, String name);
}

