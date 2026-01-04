package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "source-service", url = "${client.service.url}/api/v1/source",
        configuration = FeignConfig.class, contextId = "sourceClient")
public interface SourceClient {
    @GetMapping("/{id}")
    ResponseEntity<SourceDTO> getSourceName(@PathVariable("id") @NonNull Long sourceId);

    @GetMapping("/ids")
    ResponseEntity<List<SourceDTO>> findByNameContaining(@RequestParam("query") @NonNull String query);
}
