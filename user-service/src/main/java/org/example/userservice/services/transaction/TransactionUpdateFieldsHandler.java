package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.transaction.TransactionCategoryNotFoundException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionUpdateFieldsHandler {

    private final TransactionCategoryRepository transactionCategoryRepository;

    public void updateTransactionFields(@NonNull Transaction transaction, Long categoryId, String description,
                                        Long counterpartyId, @NonNull TransactionType type) {
        if (categoryId != null) {
            transactionCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new TransactionCategoryNotFoundException(
                            String.format("Transaction category with ID %d not found", categoryId)));
            transaction.setCategoryId(categoryId);
        }

        if (description != null) {
            transaction.setDescription(description);
        }

        if (counterpartyId != null) {
            transaction.setCounterpartyId(counterpartyId);
        } else if (transaction.getCounterpartyId() != null) {
            if (type == TransactionType.EXTERNAL_INCOME || type == TransactionType.EXTERNAL_EXPENSE) {
                transaction.setCounterpartyId(null);
            }
        }
    }
}
