package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client-type", configuration = FeignConfig.class, contextId = "clientTypeFieldApiClient")
public interface ClientTypeFieldApiClient {

    @GetMapping("/field/{fieldId}")
    ClientTypeFieldDTO getFieldById(@PathVariable Long fieldId);

    @PostMapping("/field/ids")
    List<ClientTypeFieldDTO> getFieldsByIds(@RequestBody List<Long> fieldIds);
}

