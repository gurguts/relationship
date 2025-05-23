package org.example.warehouseservice.repositories;

import org.example.warehouseservice.models.WarehouseEntry;
import org.example.warehouseservice.spec.WarehouseEntrySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseEntryRepository extends JpaRepository<WarehouseEntry, Long> {
    Page<WarehouseEntry> findAll(Specification<WarehouseEntry> specification, Pageable pageable);
}
