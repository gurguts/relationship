package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleExpenseRepository extends JpaRepository<VehicleExpense, Long> {
    List<VehicleExpense> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);
    List<VehicleExpense> findByVehicleIdInOrderByCreatedAtDesc(List<Long> vehicleIds);
    boolean existsByVehicleIdAndCategoryId(Long vehicleId, Long categoryId);
}

