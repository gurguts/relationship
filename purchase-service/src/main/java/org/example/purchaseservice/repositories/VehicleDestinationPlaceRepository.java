package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleDestinationPlaceRepository extends JpaRepository<VehicleDestinationPlace, Long> {
    boolean existsByName(String name);
}
