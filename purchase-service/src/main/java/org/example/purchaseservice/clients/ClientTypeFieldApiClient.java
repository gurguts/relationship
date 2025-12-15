package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "client-service", url = "http://localhost:8084/api/v1/client-type", configuration = FeignConfig.class, contextId = "clientTypeFieldApiClient")
public interface ClientTypeFieldApiClient {

    @GetMapping("/field/{fieldId}")
    ClientTypeFieldDTO getFieldById(@PathVariable Long fieldId);
}

