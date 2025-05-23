package org.example.userservice.restControllers.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.dto.balance.BalanceUpdateDTO;
import org.example.userservice.services.impl.IBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
public class BalanceController {
    private final IBalanceService balanceService;

    @GetMapping("/{balanceId}/{currency}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long balanceId, @PathVariable String currency) {

        BigDecimal balanceValue = balanceService.getBalanceValue(balanceId, currency);
        return ResponseEntity.ok(balanceValue);
    }

    @PatchMapping("/{balanceId}")
    public ResponseEntity<Void> updateBalance(
            @PathVariable("balanceId") Long balanceId,
            @RequestBody BalanceUpdateDTO balanceUpdateDTO) {

        balanceService.updateBalance(
                balanceId, balanceUpdateDTO.getBalanceDifference(), balanceUpdateDTO.getCurrency());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}")
    public ResponseEntity<Void> updateUserBalance(
            @PathVariable("userId") Long userId,
            @RequestBody BalanceUpdateDTO balanceUpdateDTO) {

        balanceService.updateUserBalance(
                userId, balanceUpdateDTO.getBalanceDifference(), balanceUpdateDTO.getCurrency());

        return ResponseEntity.noContent().build();
    }
}
