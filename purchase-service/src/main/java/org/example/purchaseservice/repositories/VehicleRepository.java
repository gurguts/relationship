package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByShipmentDate(LocalDate shipmentDate);

    List<Vehicle> findByUserId(Long userId);

    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate ORDER BY v.shipmentDate DESC")
    List<Vehicle> findByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    List<Vehicle> findByVehicleNumberContainingIgnoreCase(String vehicleNumber);
    
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate AND v.isOurVehicle = true ORDER BY v.shipmentDate DESC")
    List<Vehicle> findOurVehiclesByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate ORDER BY v.shipmentDate DESC")
    List<Vehicle> findAllVehiclesByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}

