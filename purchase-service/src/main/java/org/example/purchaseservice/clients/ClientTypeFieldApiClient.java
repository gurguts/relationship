package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client-type",
        configuration = FeignConfig.class, contextId = "clientTypeFieldApiClient")
public interface ClientTypeFieldApiClient {

    @GetMapping("/field/{fieldId}")
    ResponseEntity<ClientTypeFieldDTO> getFieldById(@PathVariable @NonNull Long fieldId);

    @PostMapping("/field/ids")
    ResponseEntity<List<ClientTypeFieldDTO>> getFieldsByIds(@RequestBody @NonNull List<Long> fieldIds);
}
