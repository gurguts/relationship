package org.example.userservice.clients;

import org.example.userservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "client-service", url = "${client.service.url}/api/v1/client", configuration = FeignConfig.class)
public interface ClientApiClient {
    @PostMapping("/ids")
    List<Map<Long, String>> getClients(@RequestBody List<Long> ids);
}
