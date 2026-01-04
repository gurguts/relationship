package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DriverProductBalanceRepository extends JpaRepository<DriverProductBalance, Long> {
    Optional<DriverProductBalance> findByDriverIdAndProductId(@NonNull Long driverId, @NonNull Long productId);

    @NonNull
    List<DriverProductBalance> findByDriverId(@NonNull Long driverId);

    @NonNull
    List<DriverProductBalance> findByProductId(@NonNull Long productId);

    @Query("SELECT dpb FROM DriverProductBalance dpb WHERE dpb.quantity > 0")
    @NonNull
    List<DriverProductBalance> findAllWithPositiveQuantity();
}
