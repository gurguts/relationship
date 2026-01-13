package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleTerminalRepository extends JpaRepository<VehicleTerminal, Long> {
    Optional<VehicleTerminal> findByName(String name);
    boolean existsByName(String name);
}
