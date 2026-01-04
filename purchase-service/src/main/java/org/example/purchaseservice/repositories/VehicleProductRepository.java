package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleProductRepository extends JpaRepository<VehicleProduct, Long> {
    @NonNull
    List<VehicleProduct> findByVehicleId(@NonNull Long vehicleId);

    @Query("SELECT vp FROM VehicleProduct vp WHERE vp.vehicleId IN :vehicleIds")
    @NonNull
    List<VehicleProduct> findByVehicleIdIn(@Param("vehicleIds") @NonNull List<Long> vehicleIds);
}

