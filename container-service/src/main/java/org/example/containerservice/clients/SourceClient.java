package org.example.containerservice.clients;

import org.example.containerservice.config.FeignConfig;
import org.example.containerservice.models.dto.fields.SourceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "source-service", url = "${client.service.url}/api/v1/source", configuration = FeignConfig.class)
public interface SourceClient {
    @GetMapping
    List<SourceDTO> getAllSource();
}
