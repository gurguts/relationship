package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier, Long> {
    
    List<Carrier> findByCompanyNameContainingIgnoreCase(String companyName);
}

