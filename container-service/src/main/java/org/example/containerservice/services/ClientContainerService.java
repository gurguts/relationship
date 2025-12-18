package org.example.containerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.exceptions.ContainerNotFoundException;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.repositories.ContainerBalanceRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.example.containerservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientContainerService {

    private final ClientContainerRepository clientContainerRepository;
    private final ContainerBalanceRepository containerBalanceRepository;
    private final ContainerTransactionService containerTransactionService;
    private final IContainerService containerService;

    @Transactional
    public void transferContainerToClient(Long clientId, Long containerId, BigDecimal quantity) {
        ContainerBalance balance = validateAndGetBalance(clientId, containerId, quantity);
        Long userId = balance.getUserId();
        org.example.containerservice.models.Container container = balance.getContainer();
        if (container == null) {
            throw new ContainerException("INVALID_BALANCE", "Container is null in balance");
        }

        ClientContainer clientContainer = getOrCreateClientContainer(clientId, userId, containerId, container);
        clientContainer.setQuantity(clientContainer.getQuantity().add(quantity));
        clientContainerRepository.save(clientContainer);

        balance.setClientQuantity(balance.getClientQuantity().add(quantity));
        containerBalanceRepository.save(balance);

        containerTransactionService.logTransaction(userId, null, clientId, container, quantity,
                ContainerTransactionType.TRANSFER_TO_CLIENT);

        log.info("Transferred {} units of container {} from user {} to client {}", quantity, containerId, userId, clientId);
    }

    @Transactional
    public void collectContainerFromClient(Long clientId, Long containerId, BigDecimal quantity) {
        ContainerBalance collectorBalance = validateAndGetBalance(clientId, containerId, quantity);
        Long collectingUserId = collectorBalance.getUserId();
        org.example.containerservice.models.Container container = collectorBalance.getContainer();
        if (container == null) {
            throw new ContainerException("INVALID_BALANCE", "Container is null in balance");
        }

        List<ClientContainer> clientContainers =
                clientContainerRepository.findByClientAndContainerIdOrderByUpdatedAtAsc(clientId, containerId);
        BigDecimal remainingQuantity = quantity;

        java.util.Set<Long> ownerUserIds = clientContainers.stream()
                .map(ClientContainer::getUser)
                .filter(userId -> userId != null)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, ContainerBalance> balanceMap = new java.util.HashMap<>();
        if (!ownerUserIds.isEmpty()) {
            List<ContainerBalance> balances = containerBalanceRepository.findByUserIdInAndContainerId(ownerUserIds, containerId);
            balanceMap = balances.stream()
                    .collect(java.util.stream.Collectors.toMap(ContainerBalance::getUserId, b -> b));
        }

        for (ClientContainer clientContainer : clientContainers) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) break;

            Long ownerUserId = clientContainer.getUser();
            BigDecimal availableQuantity = clientContainer.getQuantity();

            if (availableQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal collectedQuantity = availableQuantity.min(remainingQuantity);
                clientContainer.setQuantity(availableQuantity.subtract(collectedQuantity));

                if (clientContainer.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                    clientContainerRepository.delete(clientContainer);
                } else {
                    clientContainerRepository.save(clientContainer);
                }

                ContainerBalance ownerBalance = balanceMap.get(ownerUserId);
                if (ownerBalance == null) {
                    throw new ContainerNotFoundException(
                            String.format("No balance found for user %d", ownerUserId));
                }
                BigDecimal newOwnerTotal =
                        ownerBalance.getTotalQuantity().subtract(collectedQuantity).max(BigDecimal.ZERO);
                ownerBalance.setTotalQuantity(newOwnerTotal);
                ownerBalance.setClientQuantity(ownerBalance.getClientQuantity().subtract(collectedQuantity));
                containerBalanceRepository.save(ownerBalance);

                collectorBalance.setTotalQuantity(collectorBalance.getTotalQuantity().add(collectedQuantity));

                containerTransactionService.logTransaction(ownerUserId, collectingUserId, clientId, container,
                        collectedQuantity, ContainerTransactionType.COLLECT_FROM_CLIENT);
                log.info("Collected {} units of container {} from client {} (owner {}) by user {}",
                        collectedQuantity, containerId, clientId, ownerUserId, collectingUserId);

                remainingQuantity = remainingQuantity.subtract(collectedQuantity);
            }
        }

        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            collectorBalance.setTotalQuantity(collectorBalance.getTotalQuantity().add(remainingQuantity));
            containerTransactionService.logTransaction(null, collectingUserId, clientId, container,
                    remainingQuantity, ContainerTransactionType.COLLECT_FROM_CLIENT);
            log.info("Added {} units of container {} to user {} from client {} (no owner)",
                    remainingQuantity, containerId, collectingUserId, clientId);
        }

        containerBalanceRepository.save(collectorBalance);
    }

    private ContainerBalance validateAndGetBalance(Long clientId, Long containerId, BigDecimal quantity) {
        if (clientId == null || containerId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ContainerException("NOT_ENOUGH_DATA", "Client ID, container ID, and quantity must be valid");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ContainerException("AUTHENTICATION_REQUIRED", "User authentication required");
        }

        return getOrCreateBalance(userId, containerId);
    }

    @Transactional(readOnly = true)
    public List<ClientContainer> getClientContainers(Long clientId) {
        return clientContainerRepository.findByClient(clientId);
    }

    private ClientContainer getOrCreateClientContainer(Long clientId, Long userId, Long containerId,
                                                       org.example.containerservice.models.Container container) {
        return clientContainerRepository.findByClientAndUserAndContainerId(clientId, userId, containerId)
                .orElseGet(() -> {
                    ClientContainer newContainer = new ClientContainer();
                    newContainer.setClient(clientId);
                    newContainer.setUser(userId);
                    newContainer.setContainer(container);
                    newContainer.setQuantity(BigDecimal.ZERO);
                    return newContainer;
                });
    }

    private ContainerBalance getOrCreateBalance(Long userId, Long containerId) {
        return containerBalanceRepository.findByUserIdAndContainerId(userId, containerId)
                .orElseGet(() -> {
                    ContainerBalance newBalance = new ContainerBalance();
                    newBalance.setUserId(userId);
                    newBalance.setContainer(containerService.getContainerById(containerId));
                    newBalance.setTotalQuantity(BigDecimal.ZERO);
                    newBalance.setClientQuantity(BigDecimal.ZERO);
                    return newBalance;
                });
    }
}
