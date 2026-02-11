package org.example.userservice.repositories;

import feign.Param;
import lombok.NonNull;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TransactionCategoryRepository extends CrudRepository<TransactionCategory, Long> {
    @NonNull
    List<TransactionCategory> findByTypeAndIsActiveTrueOrderByNameAsc(@NonNull TransactionType type);

    Optional<TransactionCategory> findByTypeAndName(@NonNull TransactionType type, @NonNull String name);
    
    boolean existsByTypeAndName(@NonNull TransactionType type, @NonNull String name);

    @Query("SELECT c.id, c.name FROM TransactionCategory c WHERE c.id IN :ids")
    List<Object[]> findIdAndNameByIdIn(@Param("ids") Collection<Long> ids);
}

