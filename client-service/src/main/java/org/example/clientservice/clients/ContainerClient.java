package org.example.clientservice.clients;

import lombok.NonNull;
import org.example.clientservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "container-service", url = "${container.service.url}/api/v1/containers/client",
        configuration = FeignConfig.class, contextId = "containerClient")
public interface ContainerClient {
    @GetMapping("/{clientId}")
    ResponseEntity<List<Map<String, Object>>> getClientContainers(@NonNull @PathVariable("clientId") Long clientId);
}

