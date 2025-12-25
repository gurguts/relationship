package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleSenderRepository extends JpaRepository<VehicleSender, Long> {
    
    Optional<VehicleSender> findByName(String name);
    
    boolean existsByName(String name);
    
    List<VehicleSender> findAllByOrderByNameAsc();
}

