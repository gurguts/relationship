package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleTerminalRepository extends JpaRepository<VehicleTerminal, Long> {
    boolean existsByName(String name);
}
