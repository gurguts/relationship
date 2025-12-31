package org.example.clientservice.clients;

import lombok.NonNull;
import org.example.clientservice.config.FeignConfig;
import org.example.clientservice.models.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service", url = "${user.service.url}/api/v1/user",
        configuration = FeignConfig.class, contextId = "userClient")
public interface UserClient {
    @GetMapping("/{login}")
    ResponseEntity<String> getUserFullNameFromLogin(@NonNull @PathVariable("login") String login);

    @GetMapping
    ResponseEntity<List<UserDTO>> getAllUsers();
}