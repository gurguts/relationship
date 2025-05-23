package org.example.transactionservice.mappers;

import org.example.transactionservice.models.Transaction;
import org.example.transactionservice.models.dto.TransactionDTO;
import org.example.transactionservice.models.dto.TransactionCreateDTO;
import org.example.transactionservice.models.dto.TransactionOperationsDTO;
import org.example.transactionservice.models.dto.TransactionPageDTO;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDTO transactionToTransactionDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionDTO transactionDTO = new TransactionDTO();

        transactionDTO.setId(transaction.getId());
        transactionDTO.setAmount(transaction.getAmount());
        transactionDTO.setType(transaction.getType());
        transactionDTO.setDescription(transaction.getDescription());
        transactionDTO.setCreatedAt(transaction.getCreatedAt());
        transactionDTO.setClient(transaction.getClientId());
        transactionDTO.setExecutorUserId(transaction.getExecutorUserId());
        transactionDTO.setCurrency(transaction.getCurrency());

        return transactionDTO;
    }

    public Transaction transactionCreateDTOToTransaction(TransactionCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setTargetUserId(dto.getTargetUserId());
        transaction.setExecutorUserId(dto.getExecutorUserId());
        transaction.setClientId(dto.getClientId());
        transaction.setAmount(dto.getTotalPrice());
        transaction.setCurrency(dto.getCurrency());
        return transaction;
    }

    public Transaction transactionOperationsDTOToTransaction(TransactionOperationsDTO dto) {
        if (dto == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setTargetUserId(dto.getTargetUserId());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setCurrency(dto.getCurrency());
        return transaction;
    }

    public TransactionPageDTO transactionToTransactionPageDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionPageDTO transactionDTO = new TransactionPageDTO();

        transactionDTO.setTargetUserId(transaction.getTargetUserId());
        transactionDTO.setAmount(transaction.getAmount());
        transactionDTO.setType(transaction.getType().name());
        transactionDTO.setDescription(transaction.getDescription());
        transactionDTO.setCreatedAt(transaction.getCreatedAt());
        transactionDTO.setExecutorUserId(transaction.getExecutorUserId());
        transactionDTO.setCurrency(transaction.getCurrency());

        return transactionDTO;
    }
}
