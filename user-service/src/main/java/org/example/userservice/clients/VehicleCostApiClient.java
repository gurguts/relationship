package org.example.userservice.clients;

import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "purchase-service", contextId = "vehicleCostApiClient", url = "http://localhost:8093/api/v1/vehicles", configuration = FeignConfig.class)
public interface VehicleCostApiClient {
    @PostMapping("/{vehicleId}/cost")
    void updateVehicleCost(
            @PathVariable("vehicleId") Long vehicleId,
            @RequestParam("amountEur") BigDecimal amountEur,
            @RequestParam("operation") String operation
    );
}

