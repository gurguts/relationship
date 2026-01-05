package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleSenderRepository extends JpaRepository<VehicleSender, Long> {
    boolean existsByName(@NonNull String name);

    boolean existsByNameAndIdNot(@NonNull String name, @NonNull Long excludeId);

    @NonNull
    List<VehicleSender> findAllByOrderByNameAsc();
}

