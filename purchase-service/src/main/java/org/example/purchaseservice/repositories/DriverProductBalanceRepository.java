package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProductBalanceRepository extends JpaRepository<DriverProductBalance, Long> {

    Optional<DriverProductBalance> findByDriverIdAndProductId(Long driverId, Long productId);

    List<DriverProductBalance> findByDriverId(Long driverId);

    List<DriverProductBalance> findByProductId(Long productId);

    @Query("SELECT dpb FROM DriverProductBalance dpb WHERE dpb.quantity > 0")
    List<DriverProductBalance> findAllWithPositiveQuantity();

    @Query("SELECT dpb FROM DriverProductBalance dpb WHERE dpb.driverId = :driverId AND dpb.quantity > 0")
    List<DriverProductBalance> findByDriverIdWithPositiveQuantity(@Param("driverId") Long driverId);
}

