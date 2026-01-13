package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleDestinationCountryRepository extends JpaRepository<VehicleDestinationCountry, Long> {
    Optional<VehicleDestinationCountry> findByName(String name);
    boolean existsByName(String name);
}
