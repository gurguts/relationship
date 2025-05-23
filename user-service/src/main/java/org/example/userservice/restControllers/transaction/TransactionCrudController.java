package org.example.userservice.restControllers.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.dto.transaction.TransactionCreateDTO;
import org.example.userservice.models.dto.transaction.TransactionOperationsDTO;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.services.impl.ITransactionCrudService;
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
}
