package org.example.containerservice.clients;

import org.example.containerservice.config.FeignConfig;
import org.example.containerservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client-type", configuration = FeignConfig.class, contextId = "clientTypeFieldApiClient")
public interface ClientTypeFieldApiClient {

    @GetMapping("/field/{fieldId}")
    ClientTypeFieldDTO getFieldById(@PathVariable Long fieldId);
}

