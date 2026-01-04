package org.example.purchaseservice.clients;

import lombok.NonNull;
import org.example.purchaseservice.config.FeignConfig;
import org.example.purchaseservice.models.dto.purchase.TransactionPurchaseCreateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "transaction-service", url = "${user.service.url}/api/v1/transaction",
        configuration = FeignConfig.class, contextId = "transactionApiClient")
public interface TransactionApiClient {

    @PatchMapping("/{transactionId}/amount")
    ResponseEntity<Void> updateTransactionAmount(@PathVariable("transactionId") @NonNull Long transactionId,
                                                  @RequestBody @NonNull BigDecimal amount);

    @PostMapping("/purchase")
    ResponseEntity<Long> createTransactionPurchase(@RequestBody @NonNull TransactionPurchaseCreateDTO transactionPurchaseCreateDTO);

    @DeleteMapping("/{transactionId}")
    ResponseEntity<Void> deleteTransaction(@PathVariable @NonNull Long transactionId);

    @DeleteMapping("/vehicle/{vehicleId}")
    ResponseEntity<Void> deleteTransactionsByVehicleId(@PathVariable @NonNull Long vehicleId);

    @GetMapping("/vehicle/{vehicleId}")
    ResponseEntity<List<?>> getTransactionsByVehicleId(@PathVariable @NonNull Long vehicleId);

    @PostMapping("/vehicle/ids")
    ResponseEntity<Map<Long, List<?>>> getTransactionsByVehicleIds(@RequestBody @NonNull List<Long> vehicleIds);
}
