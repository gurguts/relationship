package org.example.transactionservice.clients;

import org.example.transactionservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "http://localhost:8084/api/v1/client", configuration = FeignConfig.class)
public interface ClientApiClient {
    @PostMapping("/ids")
    List<Map<Long, String>> getClients(@RequestBody List<Long> ids);
}
