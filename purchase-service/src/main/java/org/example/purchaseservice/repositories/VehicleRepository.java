package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @Override
    @NonNull
    Optional<Vehicle> findById(@NonNull Long id);

    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @Override
    @NonNull
    List<Vehicle> findAllById(@NonNull Iterable<Long> ids);

    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @Override
    @NonNull
    Page<Vehicle> findAll(Specification<Vehicle> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @NonNull
    List<Vehicle> findByShipmentDate(@NonNull LocalDate shipmentDate);

    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate AND v.isOurVehicle = true ORDER BY v.shipmentDate DESC")
    @NonNull
    List<Vehicle> findOurVehiclesByDateRange(@Param("fromDate") @NonNull LocalDate fromDate, @Param("toDate") @NonNull LocalDate toDate);

    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @Query("SELECT v FROM Vehicle v WHERE v.shipmentDate BETWEEN :fromDate AND :toDate ORDER BY v.shipmentDate DESC")
    @NonNull
    List<Vehicle> findAllVehiclesByDateRange(@Param("fromDate") @NonNull LocalDate fromDate, @Param("toDate") @NonNull LocalDate toDate);

    @EntityGraph(attributePaths = {"carrier", "sender", "receiver"})
    @Override
    @NonNull
    List<Vehicle> findAll(Specification<Vehicle> spec, @NonNull Sort sort);
}