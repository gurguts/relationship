package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "client-service", url = "http://localhost:8084/api/v1/client", configuration = FeignConfig.class)
public interface ClientApiClient {

    @PostMapping("/search")
    List<ClientDTO> searchClients(@RequestBody ClientSearchRequest request);
}
