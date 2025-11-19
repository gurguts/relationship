package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WarehouseReceiptRepository extends JpaRepository<WarehouseReceipt, Long> {
    Page<WarehouseReceipt> findAll(Specification<WarehouseReceipt> specification, Pageable pageable);

    List<WarehouseReceipt> findAll(Specification<WarehouseReceipt> specification);

    List<WarehouseReceipt> findAllByEntryDateLessThanEqual(LocalDate date);
}

