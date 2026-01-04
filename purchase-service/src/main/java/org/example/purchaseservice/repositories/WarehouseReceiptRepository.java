package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WarehouseReceiptRepository extends JpaRepository<WarehouseReceipt, Long> {
    @NonNull
    Page<WarehouseReceipt> findAll(Specification<WarehouseReceipt> specification, @NonNull Pageable pageable);

    @NonNull
    List<WarehouseReceipt> findAll(Specification<WarehouseReceipt> specification);

    @NonNull
    List<WarehouseReceipt> findAllByEntryDateLessThanEqual(@NonNull LocalDate date);
}

