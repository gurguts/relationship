package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleReceiverRepository extends JpaRepository<VehicleReceiver, Long> {
    
    Optional<VehicleReceiver> findByName(String name);
    
    boolean existsByName(String name);
    
    List<VehicleReceiver> findAllByOrderByNameAsc();
}

