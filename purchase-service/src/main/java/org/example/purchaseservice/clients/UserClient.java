package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-client", url = "${user.service.url}/api/v1/user",
        configuration = FeignConfig.class, contextId = "userClient")
public interface UserClient {
    @GetMapping("/{login}")
    ResponseEntity<String> getUserFullNameFromLogin(@PathVariable("login") @NonNull String login);

    @GetMapping
    ResponseEntity<List<UserDTO>> getAllUsers();
}