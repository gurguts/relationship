package org.example.containerservice.clients;

import org.example.containerservice.models.dto.fields.ClientProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "client-product-service", url = "${client.service.url}/api/v1/clientProduct")
public interface ClientProductClient {
    @GetMapping()
    List<ClientProductDTO> getAllClientProduct();
}
