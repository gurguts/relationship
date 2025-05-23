package org.example.userservice.restControllers.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.dto.user.UserBalanceDTO;
import org.example.userservice.services.impl.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/user-balance")
@RequiredArgsConstructor
public class UserBalancePageController {

    private final IUserService userService;

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping
    public ResponseEntity<List<UserBalanceDTO>> getUserBalances() {
        log.info("Fetching user balances");
        List<UserBalanceDTO> userBalances = userService.getUserBalances();
        return ResponseEntity.ok(userBalances);
    }
}
