package org.example.userservice.clients;

import lombok.NonNull;
import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client",
        configuration = FeignConfig.class, contextId = "clientApiClient")
public interface ClientApiClient {
    @PostMapping("/ids")
    ResponseEntity<List<Map<Long, String>>> getClients(@RequestBody @NonNull List<Long> ids);
}
