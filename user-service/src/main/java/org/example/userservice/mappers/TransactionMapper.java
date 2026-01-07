package org.example.userservice.mappers;

import lombok.NonNull;
import org.example.userservice.models.dto.transaction.TransactionCreateRequestDTO;
import org.example.userservice.models.dto.transaction.TransactionDTO;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionPageDTO transactionToTransactionPageDTO(@NonNull Transaction transaction) {
        TransactionPageDTO transactionDTO = new TransactionPageDTO();
        mapCommonFieldsToPageDTO(transaction, transactionDTO);
        transactionDTO.setType(transaction.getType().name());
        return transactionDTO;
    }

    public Transaction transactionCreateRequestDTOToTransaction(@NonNull TransactionCreateRequestDTO dto) {
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

    public TransactionDTO transactionToTransactionDTO(@NonNull Transaction transaction) {
        TransactionDTO dto = new TransactionDTO();
        mapCommonFieldsToDTO(transaction, dto);
        dto.setType(transaction.getType());
        return dto;
    }

    private void mapCommonFieldsToPageDTO(Transaction transaction, TransactionPageDTO dto) {
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setDescription(transaction.getDescription());
        dto.setCreatedAt(transaction.getCreatedAt());
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
    }

    private void mapCommonFieldsToDTO(Transaction transaction, TransactionDTO dto) {
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
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
    }
}
