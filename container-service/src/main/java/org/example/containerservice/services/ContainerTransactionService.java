package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.ContainerTransaction;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ContainerTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerTransactionService {

    private final ContainerTransactionRepository containerTransactionRepository;

    @Transactional
    public void logTransaction(Long fromUserId,
                               Long toUserId,
                               Long clientId,
                               @NonNull Container container,
                               @NonNull BigDecimal quantity,
                               @NonNull ContainerTransactionType type) {
        ContainerTransaction transaction = createTransaction(fromUserId, toUserId, clientId, container, quantity, type);
        containerTransactionRepository.save(transaction);

        log.debug("Logged transaction: fromUser={}, toUser={}, client={}, container={}, quantity={}, type={}",
                fromUserId, toUserId, clientId, container.getId(), quantity, type);
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

    @Transactional(readOnly = true)
    public List<ContainerTransaction> getAllTransactions() {
        return containerTransactionRepository.findAll();
    }
}
