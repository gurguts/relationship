package org.example.purchaseservice.clients;

import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.purchase.TransactionPurchaseCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "transaction-service", url = "${user.service.url}/api/v1/transaction",
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

    @PostMapping("/vehicle/ids")
    Map<Long, List<?>> getTransactionsByVehicleIds(@RequestBody List<Long> vehicleIds);
}
