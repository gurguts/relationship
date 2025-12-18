package org.example.userservice.restControllers.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.userservice.models.dto.transaction.TransactionDTO;
import org.example.userservice.models.dto.transaction.TransactionUpdateDTO;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.example.userservice.services.transaction.AccountTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionCrudController {
    private final ITransactionCrudService transactionCrudService;
    private final AccountTransactionService accountTransactionService;
    private final TransactionMapper transactionMapper;

    @PatchMapping("/{transactionId}/amount")
    public ResponseEntity<Void> updateTransactionAmount(
            @PathVariable("transactionId") Long transactionId,
            @RequestBody BigDecimal amount) {

        transactionCrudService.updateTransactionAmount(transactionId, amount);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('transaction:delete')")
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long transactionId) {
        transactionCrudService.delete(transactionId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@RequestBody TransactionCreateRequestDTO request) {
        Transaction transaction = transactionMapper.transactionCreateRequestDTOToTransaction(request);
        Transaction created = accountTransactionService.createTransaction(transaction);
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(created);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable Long transactionId) {
        Transaction transaction = accountTransactionService.getTransactionById(transactionId);
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(transaction);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> updateTransaction(
            @PathVariable Long transactionId,
            @RequestBody TransactionUpdateDTO updateDTO) {
        Transaction updated = accountTransactionService.updateTransaction(
                transactionId,
                updateDTO.getCategoryId(),
                updateDTO.getDescription(),
                updateDTO.getAmount(),
                updateDTO.getExchangeRate(),
                updateDTO.getCommission(),
                updateDTO.getConvertedAmount()
        );
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(updated);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view') or hasAuthority('declarant:view')")
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByVehicleId(@PathVariable Long vehicleId) {
        List<Transaction> transactions = accountTransactionService.getTransactionsByVehicleId(vehicleId);
        List<TransactionDTO> response = transactions.stream()
                .map(transactionMapper::transactionToTransactionDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view') or hasAuthority('declarant:view')")
    @PostMapping("/vehicle/ids")
    public ResponseEntity<Map<Long, List<TransactionDTO>>> getTransactionsByVehicleIds(@RequestBody List<Long> vehicleIds) {
        Map<Long, List<Transaction>> transactionsMap = accountTransactionService.getTransactionsByVehicleIds(vehicleIds);
        Map<Long, List<TransactionDTO>> response = transactionsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(transactionMapper::transactionToTransactionDTO)
                                .collect(Collectors.toList())
                ));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('warehouse:delete') or hasAuthority('declarant:delete')")
    @DeleteMapping("/vehicle/{vehicleId}")
    public ResponseEntity<Void> deleteTransactionsByVehicleId(@PathVariable Long vehicleId) {
        accountTransactionService.deleteTransactionsByVehicleId(vehicleId);
        return ResponseEntity.noContent().build();
    }

}
