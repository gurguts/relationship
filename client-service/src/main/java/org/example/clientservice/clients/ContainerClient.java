package org.example.clientservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

@FeignClient(name = "container-service", url = "http://localhost:8095/api/v1/containers/client")
public interface ContainerClient {
    @GetMapping("/{clientId}")
    ResponseEntity<List<Map<String, Object>>> getClientContainers(@PathVariable("clientId") Long clientId);
}

