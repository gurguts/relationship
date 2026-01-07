package org.example.userservice.clients;

import lombok.NonNull;
import org.example.userservice.config.FeignConfig;
import org.example.userservice.models.dto.UpdateVehicleCostRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "purchase-service", contextId = "vehicleCostApiClient",
        url = "${purchase.service.url}/api/v1/vehicles", configuration = FeignConfig.class)
public interface VehicleCostApiClient {
    @PostMapping("/{vehicleId}/cost")
    void updateVehicleCost(
            @PathVariable("vehicleId") @NonNull Long vehicleId,
            @RequestBody @NonNull UpdateVehicleCostRequest request
    );
}

