package org.example.containerservice.services.impl;

import lombok.NonNull;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.ContainerTransaction;
import org.example.containerservice.models.ContainerTransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface IContainerTransactionService {
    void logTransaction(Long fromUserId,
                        Long toUserId,
                        Long clientId,
                        @NonNull Container container,
                        @NonNull BigDecimal quantity,
                        @NonNull ContainerTransactionType type);

    @NonNull
    List<ContainerTransaction> getAllTransactions();
}
