package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.purchase.TransactionPurchaseCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "transaction-service", url = "http://localhost:8082/api/v1/transaction",
        configuration = FeignConfig.class)
public interface TransactionApiClient {

    @PatchMapping("/{transactionId}/amount")
    void updateTransactionAmount(@PathVariable("transactionId") Long transactionId, @RequestBody BigDecimal amount);

    @PostMapping("/purchase")
    Long createTransactionPurchase(@RequestBody TransactionPurchaseCreateDTO transactionPurchaseCreateDTO);

    @DeleteMapping("/{transactionId}")
    void deleteTransaction(@PathVariable Long transactionId);

    @DeleteMapping("/vehicle/{vehicleId}")
    void deleteTransactionsByVehicleId(@PathVariable Long vehicleId);

    @GetMapping("/vehicle/{vehicleId}")
    List<?> getTransactionsByVehicleId(@PathVariable Long vehicleId);
}
