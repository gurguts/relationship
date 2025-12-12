package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WarehouseWithdrawalRepository extends JpaRepository<WarehouseWithdrawal, Long> {
    Page<WarehouseWithdrawal> findAll(Specification<WarehouseWithdrawal> specification, Pageable pageable);

    List<WarehouseWithdrawal> findAllByWithdrawalDateLessThanEqual(LocalDate date);
    
    List<WarehouseWithdrawal> findByVehicleId(Long vehicleId);
}