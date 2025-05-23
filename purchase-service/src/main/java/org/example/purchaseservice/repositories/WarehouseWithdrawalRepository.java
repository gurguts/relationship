package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.WarehouseWithdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseWithdrawalRepository extends JpaRepository<WarehouseWithdrawal, Long> {
    Page<WarehouseWithdrawal> findAll(Specification<WarehouseWithdrawal> specification, Pageable pageable);
}