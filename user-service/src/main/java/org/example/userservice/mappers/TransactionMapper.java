package org.example.userservice.mappers;

import org.example.userservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.userservice.models.dto.transaction.TransactionDTO;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionPageDTO transactionToTransactionPageDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionPageDTO transactionDTO = new TransactionPageDTO();

        transactionDTO.setId(transaction.getId());
        transactionDTO.setTargetUserId(null);
        transactionDTO.setAmount(transaction.getAmount());
        transactionDTO.setType(transaction.getType().name());
        transactionDTO.setDescription(transaction.getDescription());
        transactionDTO.setCreatedAt(transaction.getCreatedAt());
        transactionDTO.setExecutorUserId(transaction.getExecutorUserId());
        transactionDTO.setCurrency(transaction.getCurrency());
        transactionDTO.setFromAccountId(transaction.getFromAccountId());
        transactionDTO.setToAccountId(transaction.getToAccountId());
        transactionDTO.setCategoryId(transaction.getCategoryId());
        transactionDTO.setExchangeRate(transaction.getExchangeRate());
        transactionDTO.setConvertedCurrency(transaction.getConvertedCurrency());
        transactionDTO.setConvertedAmount(transaction.getConvertedAmount());
        transactionDTO.setCommission(transaction.getCommission());
        transactionDTO.setVehicleId(transaction.getVehicleId());
        transactionDTO.setCounterpartyId(transaction.getCounterpartyId());

        return transactionDTO;
    }

    public Transaction transactionCreateRequestDTOToTransaction(TransactionCreateRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setType(dto.getType());
        transaction.setCategoryId(dto.getCategoryId());
        transaction.setFromAccountId(dto.getFromAccountId());
        transaction.setToAccountId(dto.getToAccountId());
        transaction.setAmount(dto.getAmount());
        transaction.setCurrency(dto.getCurrency());
        transaction.setConvertedCurrency(dto.getConvertedCurrency());
        transaction.setExchangeRate(dto.getExchangeRate());
        transaction.setClientId(dto.getClientId());
        transaction.setDescription(dto.getDescription());
        transaction.setCommission(dto.getCommission());
        transaction.setVehicleId(dto.getVehicleId());
        transaction.setCounterpartyId(dto.getCounterpartyId());

        return transaction;
    }

    public TransactionDTO transactionToTransactionDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionDTO dto = new TransactionDTO();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setClientId(transaction.getClientId());
        dto.setExecutorUserId(transaction.getExecutorUserId());
        dto.setCurrency(transaction.getCurrency());
        dto.setFromAccountId(transaction.getFromAccountId());
        dto.setToAccountId(transaction.getToAccountId());
        dto.setCategoryId(transaction.getCategoryId());
        dto.setExchangeRate(transaction.getExchangeRate());
        dto.setConvertedCurrency(transaction.getConvertedCurrency());
        dto.setConvertedAmount(transaction.getConvertedAmount());
        dto.setCommission(transaction.getCommission());
        dto.setVehicleId(transaction.getVehicleId());
        dto.setCounterpartyId(transaction.getCounterpartyId());

        return dto;
    }
}
