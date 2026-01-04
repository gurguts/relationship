package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WarehouseWithdrawalRepository extends JpaRepository<WarehouseWithdrawal, Long> {
    @NonNull
    Page<WarehouseWithdrawal> findAll(Specification<WarehouseWithdrawal> specification, @NonNull Pageable pageable);

    @NonNull
    List<WarehouseWithdrawal> findAllByWithdrawalDateLessThanEqual(@NonNull LocalDate date);
}