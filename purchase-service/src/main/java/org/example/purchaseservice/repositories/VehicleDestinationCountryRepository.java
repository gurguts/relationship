package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleDestinationCountryRepository extends JpaRepository<VehicleDestinationCountry, Long> {
    boolean existsByName(String name);
}
