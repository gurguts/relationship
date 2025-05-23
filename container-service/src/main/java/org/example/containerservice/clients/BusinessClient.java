package org.example.containerservice.clients;

import org.example.containerservice.models.dto.fields.BusinessDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "business-service", url = "http://localhost:8084/api/v1/business")
public interface BusinessClient {
    @GetMapping()
    List<BusinessDTO> getAllBusiness();
}
