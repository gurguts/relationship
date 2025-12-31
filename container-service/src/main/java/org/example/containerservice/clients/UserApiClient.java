package org.example.containerservice.clients;

import org.example.containerservice.config.FeignConfig;
import org.example.containerservice.models.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "user-service", url = "${user.service.url}/api/v1/user", configuration = FeignConfig.class)
public interface UserApiClient {
    @GetMapping()
    List<UserDTO> getAllUsers();
}