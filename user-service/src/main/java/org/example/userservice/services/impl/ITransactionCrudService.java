package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.Transaction;

import java.math.BigDecimal;

public interface ITransactionCrudService {
    Transaction getTransaction(@NonNull Long id);

    void updateTransactionAmount(@NonNull Long transactionId, @NonNull BigDecimal amount);

    void delete(@NonNull Long transactionId);
}
