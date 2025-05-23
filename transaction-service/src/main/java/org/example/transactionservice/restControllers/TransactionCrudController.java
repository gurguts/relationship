package org.example.transactionservice.restControllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.mappers.TransactionMapper;
import org.example.transactionservice.models.Transaction;
import org.example.transactionservice.models.dto.TransactionCreateDTO;
import org.example.transactionservice.models.dto.TransactionOperationsDTO;
import org.example.transactionservice.services.impl.ITransactionCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.TransactionOperations;
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
        Transaction savedTransaction = transactionCrudService.createSaleTransaction(transaction, transactionCreateDTO.getProductId());
        return ResponseEntity.ok(savedTransaction.getId());
    }

    @PostMapping("/purchase")
    public ResponseEntity<Long> createTransactionPurchase(@RequestBody TransactionCreateDTO transactionCreateDTO) {
        Transaction transaction = transactionMapper.transactionCreateDTOToTransaction(transactionCreateDTO);
        Transaction savedTransaction = transactionCrudService.createPurchaseTransaction(transaction, transactionCreateDTO.getProductId());
        return ResponseEntity.ok(savedTransaction.getId());
    }

    @PreAuthorize("hasAuthority('finance:balance_edit')")
    @PostMapping("/deposit")
    public ResponseEntity<Long> createDepositTransaction(@RequestBody TransactionOperationsDTO transactionOperationsDTO) {
        Transaction transaction = transactionMapper.transactionOperationsDTOToTransaction(transactionOperationsDTO);
        Transaction savedTransaction = transactionCrudService.createDepositTransaction(transaction);
        return ResponseEntity.ok(savedTransaction.getId());
    }

    @PreAuthorize("hasAuthority('finance:balance_edit')")
    @PostMapping("/withdraw")
    public ResponseEntity<Long> createWithdrawTransaction(@RequestBody TransactionOperationsDTO transactionOperationsDTO) {
        Transaction transaction = transactionMapper.transactionOperationsDTOToTransaction(transactionOperationsDTO);
        Transaction savedTransaction = transactionCrudService.createWithdrawTransaction(transaction);
        return ResponseEntity.ok(savedTransaction.getId());
    }
}
