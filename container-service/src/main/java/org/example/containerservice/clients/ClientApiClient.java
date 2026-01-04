package org.example.containerservice.clients;

import lombok.NonNull;
import org.example.containerservice.config.FeignConfig;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client",
        configuration = FeignConfig.class, contextId = "clientApiClient")
public interface ClientApiClient {

    @PostMapping("/search")
    ResponseEntity<List<ClientDTO>> searchClients(@NonNull @RequestBody ClientSearchRequest request);
    
    @PostMapping("/field-values/batch")
    ResponseEntity<Map<Long, List<ClientFieldValueDTO>>> getClientFieldValuesBatch(@NonNull @RequestBody List<Long> clientIds);
}
