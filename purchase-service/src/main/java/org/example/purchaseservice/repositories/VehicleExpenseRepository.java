package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleExpenseRepository extends JpaRepository<VehicleExpense, Long> {
    @NonNull
    List<VehicleExpense> findByVehicleIdOrderByCreatedAtDesc(@NonNull Long vehicleId);

    @NonNull
    List<VehicleExpense> findByVehicleIdInOrderByCreatedAtDesc(@NonNull List<Long> vehicleIds);

    boolean existsByVehicleIdAndCategoryId(@NonNull Long vehicleId, @NonNull Long categoryId);
}

