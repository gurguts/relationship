package org.example.userservice.restControllers.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.userservice.models.dto.transaction.TransactionDTO;
import org.example.userservice.models.dto.transaction.TransactionUpdateDTO;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.example.userservice.services.impl.IAccountTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionCrudController {
    private final ITransactionCrudService transactionCrudService;
    private final IAccountTransactionService accountTransactionService;
    private final TransactionMapper transactionMapper;

    @PreAuthorize("hasAuthority('finance:view')")
    @PatchMapping("/{transactionId}/amount")
    public ResponseEntity<Void> updateTransactionAmount(
            @PathVariable @Positive @NonNull Long transactionId,
            @RequestBody @DecimalMin(value = "0.0", inclusive = false) @NonNull BigDecimal amount) {
        transactionCrudService.updateTransactionAmount(transactionId, amount);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @PutMapping("/{transactionId}/amount")
    public ResponseEntity<Void> updateTransactionAmountPut(
            @PathVariable @Positive @NonNull Long transactionId,
            @RequestBody @DecimalMin(value = "0.0", inclusive = false) @NonNull BigDecimal amount) {
        transactionCrudService.updateTransactionAmount(transactionId, amount);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('transaction:delete')")
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable @Positive @NonNull Long transactionId) {
        transactionCrudService.delete(transactionId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(
            @RequestBody @Valid @NonNull TransactionCreateRequestDTO request) {
        Transaction transaction = transactionMapper.transactionCreateRequestDTOToTransaction(request);
        Transaction created = accountTransactionService.createTransaction(transaction);
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.status(CREATED).location(location).body(response);
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> getTransaction(@PathVariable @Positive @NonNull Long transactionId) {
        Transaction transaction = accountTransactionService.getTransactionById(transactionId);
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(transaction);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @PatchMapping("/{transactionId}")
    public ResponseEntity<TransactionDTO> updateTransaction(
            @PathVariable @Positive @NonNull Long transactionId,
            @RequestBody @Valid @NonNull TransactionUpdateDTO updateDTO) {
        Transaction updated = accountTransactionService.updateTransaction(
                transactionId,
                updateDTO.getCategoryId(),
                updateDTO.getDescription(),
                updateDTO.getAmount(),
                updateDTO.getExchangeRate(),
                updateDTO.getCommission(),
                updateDTO.getConvertedAmount(),
                updateDTO.getCounterpartyId()
        );
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(updated);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view') or hasAuthority('declarant:view')")
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByVehicleId(
            @PathVariable @Positive @NonNull Long vehicleId) {
        List<Transaction> transactions = accountTransactionService.getTransactionsByVehicleId(vehicleId);
        List<TransactionDTO> response = transactions.stream()
                .map(transactionMapper::transactionToTransactionDTO)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('finance:view') or hasAuthority('declarant:view')")
    @PostMapping("/vehicle/ids")
    public ResponseEntity<Map<Long, List<TransactionDTO>>> getTransactionsByVehicleIds(
            @RequestBody @Valid @NotEmpty @NonNull List<@Positive Long> vehicleIds) {
        Map<Long, List<Transaction>> transactionsMap = accountTransactionService.getTransactionsByVehicleIds(vehicleIds);
        Map<Long, List<TransactionDTO>> response = transactionsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(transactionMapper::transactionToTransactionDTO)
                                .toList()
                ));
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('warehouse:delete') or hasAuthority('declarant:delete')")
    @DeleteMapping("/vehicle/{vehicleId}")
    public ResponseEntity<Void> deleteTransactionsByVehicleId(@PathVariable @Positive @NonNull Long vehicleId) {
        accountTransactionService.deleteTransactionsByVehicleId(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
