package org.example.userservice.clients;

import lombok.NonNull;
import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "purchase-service", contextId = "vehicleApiClient",
        url = "${purchase.service.url}/api/v1/vehicles", configuration = FeignConfig.class)
public interface VehicleApiClient {
    @PostMapping("/ids")
    ResponseEntity<List<Map<Long, String>>> getVehicles(@RequestBody @NonNull List<Long> ids);
    
    @GetMapping("/{vehicleId}/number")
    ResponseEntity<String> getVehicleNumber(@PathVariable("vehicleId") @NonNull Long vehicleId);
}

