package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.balance.VehicleProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleProductRepository extends JpaRepository<VehicleProduct, Long> {
    
    List<VehicleProduct> findByVehicleId(Long vehicleId);
    
    @Query("SELECT vp FROM VehicleProduct vp WHERE vp.vehicleId IN :vehicleIds")
    List<VehicleProduct> findByVehicleIdIn(@Param("vehicleIds") List<Long> vehicleIds);
    
    void deleteByVehicleId(Long vehicleId);
    
    @Query("SELECT vp FROM VehicleProduct vp WHERE vp.vehicleId = :vehicleId AND vp.productId = :productId")
    List<VehicleProduct> findByVehicleIdAndProductId(@Param("vehicleId") Long vehicleId, @Param("productId") Long productId);
}

