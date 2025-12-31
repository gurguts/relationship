package org.example.containerservice.clients;

import org.example.containerservice.models.dto.fields.StatusDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "status-service", url = "${client.service.url}/api/v1/status")
public interface StatusClient {
    @GetMapping
    List<StatusDTO> getAllStatus();
}
