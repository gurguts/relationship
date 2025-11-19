package org.example.userservice.restControllers.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.dto.transaction.TransactionCreateDTO;
import org.example.userservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.userservice.models.dto.transaction.TransactionDTO;
import org.example.userservice.models.dto.transaction.TransactionOperationsDTO;
import org.example.userservice.models.dto.transaction.TransactionUpdateDTO;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.services.impl.ITransactionCrudService;
import org.example.userservice.services.transaction.AccountTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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

    @PostMapping("/sale")
    public ResponseEntity<Long> createTransactionSale(@RequestBody TransactionCreateDTO transactionCreateDTO) {
        Transaction transaction = transactionMapper.transactionCreateDTOToTransaction(transactionCreateDTO);
        Transaction savedTransaction = transactionCrudService.createSaleTransaction(transaction,
                transactionCreateDTO.getProductId());
        return ResponseEntity.ok(savedTransaction.getId());
    }

    @PostMapping("/purchase")
    public ResponseEntity<Long> createTransactionPurchase(@RequestBody TransactionCreateDTO transactionCreateDTO) {
        Transaction transaction = transactionMapper.transactionCreateDTOToTransaction(transactionCreateDTO);
        Transaction savedTransaction = transactionCrudService.createPurchaseTransaction(transaction,
                transactionCreateDTO.getProductId());
        return ResponseEntity.ok(savedTransaction.getId());
    }

    @PreAuthorize("hasAuthority('finance:balance_edit')")
    @PostMapping("/deposit")
    public ResponseEntity<Long> createDepositTransaction(
            @RequestBody TransactionOperationsDTO transactionOperationsDTO) {
        Transaction transaction = transactionMapper.transactionOperationsDTOToTransaction(transactionOperationsDTO);
        Transaction savedTransaction = transactionCrudService.createDepositTransaction(transaction);
        return ResponseEntity.ok(savedTransaction.getId());
    }

    @PreAuthorize("hasAuthority('finance:balance_edit')")
    @PostMapping("/withdraw")
    public ResponseEntity<Long> createWithdrawTransaction(
            @RequestBody TransactionOperationsDTO transactionOperationsDTO) {
        Transaction transaction = transactionMapper.transactionOperationsDTOToTransaction(transactionOperationsDTO);
        Transaction savedTransaction = transactionCrudService.createWithdrawTransaction(transaction);
        return ResponseEntity.ok(savedTransaction.getId());
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
                updateDTO.getExchangeRate()
        );
        TransactionDTO response = transactionMapper.transactionToTransactionDTO(updated);
        return ResponseEntity.ok(response);
    }

}
