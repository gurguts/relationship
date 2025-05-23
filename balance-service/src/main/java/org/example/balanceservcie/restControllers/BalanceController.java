package org.example.balanceservcie.restControllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.balanceservcie.models.Balance;
import org.example.balanceservcie.models.dto.BalanceUpdateDTO;
import org.example.balanceservcie.services.impl.IBalanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
public class BalanceController {
    private final IBalanceService balanceService;

    @Value("${auth.secret.internal}")
    private String secret;

    @GetMapping("/secret/{userId}")
    public ResponseEntity<BigDecimal> getSecretBalance(@PathVariable Long userId,
                                                 @RequestHeader(value = "X-Internal-Request", required = false) String internalHeader) {
        if (!secret.equals(internalHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BigDecimal balanceValue = balanceService.getUserBalance(userId, "UAH");
        return ResponseEntity.ok(balanceValue);
    }

    @GetMapping("/{balanceId}/{currency}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long balanceId, @PathVariable String currency) {

        BigDecimal balanceValue = balanceService.getBalanceValue(balanceId, currency);
        return ResponseEntity.ok(balanceValue);
    }

    @PatchMapping("/{balanceId}")
    public ResponseEntity<Void> updateBalance(
            @PathVariable("balanceId") Long balanceId,
            @RequestBody BalanceUpdateDTO balanceUpdateDTO) {

        balanceService.updateBalance(balanceId, balanceUpdateDTO.getBalanceDifference(), balanceUpdateDTO.getCurrency());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/user/{userId}")
    public ResponseEntity<Void> updateUserBalance(
            @PathVariable("userId") Long userId,
            @RequestBody BalanceUpdateDTO balanceUpdateDTO) {

        balanceService.updateUserBalance(userId, balanceUpdateDTO.getBalanceDifference(), balanceUpdateDTO.getCurrency());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, BigDecimal>> getUserBalances(@PathVariable("userId") Long userId) {
        Balance balance = balanceService.getBalanceByUserId(userId);
        Map<String, BigDecimal> balances = new HashMap<>();
        balances.put("UAH", balance.getBalanceUAH());
        balances.put("EUR", balance.getBalanceEUR());
        balances.put("USD", balance.getBalanceUSD());
        return ResponseEntity.ok(balances);
    }

    @PreAuthorize("hasAuthority('user:create')")
    @PostMapping("/user/{userId}")
    public ResponseEntity<Void> createUserBalance(@PathVariable("userId") Long userId) {
        Long balanceId = balanceService.createUserBalance(userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("/api/v1/balance/" + balanceId));
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('user:delete')")
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteBalanceUser(@PathVariable Long userId){
        log.info("Deleting balance for userId={}", userId);
        balanceService.deleteBalanceUser(userId);
        return ResponseEntity.noContent().build();
    }
}
