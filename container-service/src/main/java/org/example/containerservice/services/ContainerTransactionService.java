package org.example.containerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.models.ContainerTransaction;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ContainerTransactionRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContainerTransactionService {

    private final ContainerTransactionRepository containerTransactionRepository;
    private final IContainerService containerService;

    @Transactional
    public void logTransaction(Long fromUserId, Long toUserId, Long clientId, Long containerId,
                               BigDecimal quantity, ContainerTransactionType type) {
        ContainerTransaction transaction = new ContainerTransaction();
        transaction.setFromUserId(fromUserId);
        transaction.setToUserId(toUserId);
        transaction.setClientId(clientId);
        transaction.setContainer(containerService.getContainerById(containerId));
        transaction.setQuantity(quantity);
        transaction.setType(type);
        containerTransactionRepository.save(transaction);

        log.debug("Logged transaction: fromUser={}, toUser={}, client={}, container={}, quantity={}, type={}",
                fromUserId, toUserId, clientId, containerId, quantity, type);
    }

    public List<ContainerTransaction> getAllTransactions() {
        return containerTransactionRepository.findAll();
    }
}
