package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.WarehouseEntry;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WarehouseEntryRepository extends JpaRepository<WarehouseEntry, Long> {
    Page<WarehouseEntry> findAll(Specification<WarehouseEntry> specification, Pageable pageable);

    List<WarehouseEntry> findAll(Specification<WarehouseEntry> specification);

    Optional<WarehouseEntry> findByUserIdAndProductIdAndEntryDate(Long userId, Long productId, LocalDate entryDate);

    Optional<WarehouseEntry> findByUserIdAndProductIdAndEntryDateAndType(Long userId, Long productId, LocalDate entryDate, WithdrawalReason type);

    List<WarehouseEntry> findAllByEntryDateLessThanEqual(LocalDate date);
}
