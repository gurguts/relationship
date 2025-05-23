package org.example.containerservice.clients;

import org.example.containerservice.models.dto.fields.RouteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "route-service", url = "http://localhost:8084/api/v1/route")
public interface RouteClient {
    @GetMapping
    List<RouteDTO> getAllRoute();
}
