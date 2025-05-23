package org.example.purchaseservice.clients;

import org.example.purchaseservice.models.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service", url = "http://localhost:8082/api/v1/user")
public interface UserClient {
    @GetMapping("/{login}")
    String getUserFullNameFromLogin(@PathVariable("login") String login);

    @GetMapping
    List<UserDTO> getAllUsers();
}