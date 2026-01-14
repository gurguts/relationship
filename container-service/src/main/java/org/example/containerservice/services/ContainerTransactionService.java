package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.ContainerTransaction;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ContainerTransactionRepository;
import org.example.containerservice.services.impl.IContainerTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerTransactionService implements IContainerTransactionService {

    private final ContainerTransactionRepository containerTransactionRepository;

    @Override
    @Transactional
    public void logTransaction(Long fromUserId,
                               Long toUserId,
                               Long clientId,
                               @NonNull Container container,
                               @NonNull BigDecimal quantity,
                               @NonNull ContainerTransactionType type) {
        ContainerTransaction transaction = createTransaction(fromUserId, toUserId, clientId, container, quantity, type);
        containerTransactionRepository.save(transaction);
    }

    private ContainerTransaction createTransaction(Long fromUserId,
                                                   Long toUserId,
                                                   Long clientId,
                                                   @NonNull Container container,
                                                   @NonNull BigDecimal quantity,
                                                   @NonNull ContainerTransactionType type) {
        ContainerTransaction transaction = new ContainerTransaction();
        transaction.setFromUserId(fromUserId);
        transaction.setToUserId(toUserId);
        transaction.setClientId(clientId);
        transaction.setContainer(container);
        transaction.setQuantity(quantity);
        transaction.setType(type);
        return transaction;
    }

    @Override
    @Transactional(readOnly = true)
    @NonNull
    public List<ContainerTransaction> getAllTransactions() {
        return containerTransactionRepository.findAll();
    }
}
