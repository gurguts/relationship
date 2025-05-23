package org.example.authservice.clients;

import org.example.authservice.models.UserDet;
import org.example.authservice.models.dto.UserAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Optional;

@FeignClient(name = "user-service", url = "http://localhost:8082/api/v1/user")
public interface UserServiceClient {

    @GetMapping("/auth/{login}")
    Optional<UserAuthDTO> getUserAuthByLogin(@PathVariable String login,
                                             @RequestHeader("X-Internal-Request") String internalHeader);

    @GetMapping("/details/{login}")
    Optional<UserDet> getUserDetByLogin(@PathVariable String login,
                                           @RequestHeader("X-Internal-Request") String internalHeader);
}
