package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    @NonNull
    Page<Transaction> findAll(Specification<Transaction> specification, @NonNull Pageable pageable);
    
    @NonNull
    List<Transaction> findByVehicleIdOrderByCreatedAtDesc(@NonNull Long vehicleId);
    
    @NonNull
    @Query("SELECT t FROM Transaction t WHERE t.vehicleId IN :vehicleIds ORDER BY t.createdAt DESC")
    List<Transaction> findByVehicleIdInOrderByCreatedAtDesc(@Param("vehicleIds") @NonNull List<Long> vehicleIds);
}
