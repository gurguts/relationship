package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    @EntityGraph(attributePaths = {"carrier"})
    @Override
    Page<Vehicle> findAll(Specification<Vehicle> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"carrier"})
    List<Vehicle> findByShipmentDate(LocalDate shipmentDate);

    @EntityGraph(attributePaths = {"carrier"})
    List<Vehicle> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"carrier"})
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate ORDER BY v.shipmentDate DESC")
    List<Vehicle> findByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @EntityGraph(attributePaths = {"carrier"})
    List<Vehicle> findByVehicleNumberContainingIgnoreCase(String vehicleNumber);
    
    @EntityGraph(attributePaths = {"carrier"})
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate AND v.isOurVehicle = true ORDER BY v.shipmentDate DESC")
    List<Vehicle> findOurVehiclesByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
    
    @EntityGraph(attributePaths = {"carrier"})
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate ORDER BY v.shipmentDate DESC")
    List<Vehicle> findAllVehiclesByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @EntityGraph(attributePaths = {"carrier"})
    @Override
    List<Vehicle> findAll(org.springframework.data.jpa.domain.Specification<Vehicle> spec, org.springframework.data.domain.Sort sort);
}

