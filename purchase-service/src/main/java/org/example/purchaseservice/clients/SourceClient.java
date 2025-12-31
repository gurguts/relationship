package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "source-service", url = "${client.service.url}/api/v1/source", configuration = FeignConfig.class)
public interface SourceClient {
    @GetMapping("/{id}")
    SourceDTO getSourceName(@PathVariable("id") Long sourceId);

    @GetMapping("/ids")
    List<SourceDTO> findByNameContaining(@RequestParam("query") String query);
}
