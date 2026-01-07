package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionCategoryRepository extends CrudRepository<TransactionCategory, Long> {
    @NonNull
    List<TransactionCategory> findByTypeAndIsActiveTrueOrderByNameAsc(@NonNull TransactionType type);
    
    @NonNull
    List<TransactionCategory> findByTypeOrderByNameAsc(@NonNull TransactionType type);
    
    Optional<TransactionCategory> findByTypeAndName(@NonNull TransactionType type, @NonNull String name);
    
    boolean existsByTypeAndName(@NonNull TransactionType type, @NonNull String name);
}

