package org.example.transactionservice.clients;

import org.example.transactionservice.config.FeignConfig;
import org.example.transactionservice.models.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "user-service", url = "http://localhost:8082/api/v1/user", configuration = FeignConfig.class)
public interface UserApiClient {

/*
    @PatchMapping("/{userId}/balance")
    void updateUserBalance(@PathVariable("userId") Long userId, @RequestBody BigDecimal balanceDifference);
*/

    @GetMapping
    List<UserDTO> getUsers();
}