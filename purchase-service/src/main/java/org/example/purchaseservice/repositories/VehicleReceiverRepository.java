package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleReceiverRepository extends JpaRepository<VehicleReceiver, Long> {
    boolean existsByName(@NonNull String name);

    boolean existsByNameAndIdNot(@NonNull String name, @NonNull Long excludeId);

    @NonNull
    List<VehicleReceiver> findAllByOrderByNameAsc();
}

