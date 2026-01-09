package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client",
        configuration = FeignConfig.class, contextId = "clientApiClient")
public interface ClientApiClient {

    @PostMapping("/search")
    ResponseEntity<List<ClientDTO>> searchClients(@RequestBody @NonNull ClientSearchRequest request);
    
    @GetMapping("/{clientId}/field-values")
    ResponseEntity<List<ClientFieldValueDTO>> getClientFieldValues(@PathVariable @NonNull Long clientId);
    
    @PostMapping("/field-values/batch")
    ResponseEntity<Map<Long, List<ClientFieldValueDTO>>> getClientFieldValuesBatch(@RequestBody @NonNull org.example.purchaseservice.models.dto.client.ClientIdsRequest request);
    
    @PostMapping("/ids/search")
    ResponseEntity<List<Long>> searchClientIds(@RequestBody @NonNull ClientSearchRequest request);
    
    @PostMapping("/by-ids")
    ResponseEntity<List<ClientDTO>> getClientsByIds(@RequestBody @NonNull List<Long> clientIds);
}
