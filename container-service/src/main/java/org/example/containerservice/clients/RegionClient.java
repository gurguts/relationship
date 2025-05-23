package org.example.containerservice.clients;

import org.example.containerservice.models.dto.fields.RegionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "region-service", url = "http://localhost:8084/api/v1/region")
public interface RegionClient {
    @GetMapping
    List<RegionDTO> getAllRegion();
}
