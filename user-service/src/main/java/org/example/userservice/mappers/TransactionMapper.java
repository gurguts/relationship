package org.example.userservice.mappers;

import org.example.userservice.models.dto.transaction.TransactionCreateDTO;
import org.example.userservice.models.dto.transaction.TransactionOperationsDTO;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

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
        transaction.setCurrency(dto.getCurrency());
        transaction.setDescription(dto.getDescription());
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
