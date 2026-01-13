package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleDestinationPlaceRepository extends JpaRepository<VehicleDestinationPlace, Long> {
    Optional<VehicleDestinationPlace> findByName(String name);
    boolean existsByName(String name);
}
