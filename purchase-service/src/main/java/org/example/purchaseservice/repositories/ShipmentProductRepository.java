package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.ShipmentProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentProductRepository extends JpaRepository<ShipmentProduct, Long> {
    
    List<ShipmentProduct> findByShipmentId(Long shipmentId);
    
    void deleteByShipmentId(Long shipmentId);
}

