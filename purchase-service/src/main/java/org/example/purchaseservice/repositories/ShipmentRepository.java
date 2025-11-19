package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByShipmentDate(LocalDate shipmentDate);

    List<Shipment> findByUserId(Long userId);

    @Query("SELECT s FROM Shipment s WHERE s.shipmentDate BETWEEN :fromDate AND :toDate ORDER BY s.shipmentDate DESC")
    List<Shipment> findByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    List<Shipment> findByVehicleNumberContainingIgnoreCase(String vehicleNumber);
}

