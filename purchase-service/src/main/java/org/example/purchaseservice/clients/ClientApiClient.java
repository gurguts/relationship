package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client", configuration = FeignConfig.class, contextId = "clientApiClient")
public interface ClientApiClient {

    @PostMapping("/search")
    List<ClientDTO> searchClients(@RequestBody ClientSearchRequest request);
    
    @GetMapping("/{clientId}/field-values")
    List<ClientFieldValueDTO> getClientFieldValues(@PathVariable Long clientId);
    
    @PostMapping("/field-values/batch")
    Map<Long, List<ClientFieldValueDTO>> getClientFieldValuesBatch(@RequestBody List<Long> clientIds);
}
