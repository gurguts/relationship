package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.exceptions.ContainerNotFoundException;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.repositories.ContainerBalanceRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.example.containerservice.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerService {

    private static final String ERROR_INVALID_BALANCE = "INVALID_BALANCE";
    private static final String ERROR_NOT_ENOUGH_DATA = "NOT_ENOUGH_DATA";
    private static final String ERROR_AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    private static final String MESSAGE_CONTAINER_NULL = "Container is null in balance";
    private static final String MESSAGE_VALIDATION_REQUIRED = "Client ID, container ID, and quantity must be valid";
    private static final String MESSAGE_AUTHENTICATION_REQUIRED = "User authentication required";
    private static final String MESSAGE_BALANCE_NOT_FOUND = "No balance found for user %d";

    private final ClientContainerRepository clientContainerRepository;
    private final ContainerBalanceRepository containerBalanceRepository;
    private final ContainerTransactionService containerTransactionService;
    private final IContainerService containerService;

    @Transactional
    public void transferContainerToClient(@NonNull Long userId,
                                          @NonNull Long clientId,
                                          @NonNull Long containerId,
                                          @NonNull BigDecimal quantity) {
        ContainerBalance balance = validateAndGetBalance(userId, containerId, quantity);
        Container container = balance.getContainer();
        if (container == null) {
            throw new ContainerException(ERROR_INVALID_BALANCE, MESSAGE_CONTAINER_NULL);
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
    public void collectContainerFromClient(@NonNull Long userId,
                                           @NonNull Long clientId,
                                           @NonNull Long containerId,
                                           @NonNull BigDecimal quantity) {
        ContainerBalance collectorBalance = validateAndGetBalance(userId, containerId, quantity);
        Container container = collectorBalance.getContainer();
        if (container == null) {
            throw new ContainerException(ERROR_INVALID_BALANCE, MESSAGE_CONTAINER_NULL);
        }

        List<ClientContainer> clientContainers =
                clientContainerRepository.findByClientAndContainerIdOrderByUpdatedAtAsc(clientId, containerId);
        BigDecimal remainingQuantity = quantity;

        Set<Long> ownerUserIds = clientContainers.stream()
                .map(ClientContainer::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, ContainerBalance> balanceMap = new HashMap<>();
        if (!ownerUserIds.isEmpty()) {
            List<ContainerBalance> balances = containerBalanceRepository.findByUserIdInAndContainerId(ownerUserIds, containerId);
            balanceMap = balances.stream()
                    .collect(Collectors.toMap(ContainerBalance::getUserId, balance -> balance));
        }

        for (ClientContainer clientContainer : clientContainers) {
            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

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
                    throw new ContainerNotFoundException(String.format(MESSAGE_BALANCE_NOT_FOUND, ownerUserId));
                }
                BigDecimal newOwnerTotal =
                        ownerBalance.getTotalQuantity().subtract(collectedQuantity).max(BigDecimal.ZERO);
                ownerBalance.setTotalQuantity(newOwnerTotal);
                ownerBalance.setClientQuantity(ownerBalance.getClientQuantity().subtract(collectedQuantity));
                containerBalanceRepository.save(ownerBalance);

                collectorBalance.setTotalQuantity(collectorBalance.getTotalQuantity().add(collectedQuantity));

                containerTransactionService.logTransaction(ownerUserId, userId, clientId, container,
                        collectedQuantity, ContainerTransactionType.COLLECT_FROM_CLIENT);
                log.info("Collected {} units of container {} from client {} (owner {}) by user {}",
                        collectedQuantity, containerId, clientId, ownerUserId, userId);

                remainingQuantity = remainingQuantity.subtract(collectedQuantity);
            }
        }

        if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
            collectorBalance.setTotalQuantity(collectorBalance.getTotalQuantity().add(remainingQuantity));
            containerTransactionService.logTransaction(null, userId, clientId, container,
                    remainingQuantity, ContainerTransactionType.COLLECT_FROM_CLIENT);
            log.info("Added {} units of container {} to user {} from client {} (no owner)",
                    remainingQuantity, containerId, userId, clientId);
        }

        containerBalanceRepository.save(collectorBalance);
    }

    private ContainerBalance validateAndGetBalance(@NonNull Long userId,
                                                   @NonNull Long containerId,
                                                   @NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ContainerException(ERROR_NOT_ENOUGH_DATA, MESSAGE_VALIDATION_REQUIRED);
        }

        if (userId == null) {
            throw new ContainerException(ERROR_AUTHENTICATION_REQUIRED, MESSAGE_AUTHENTICATION_REQUIRED);
        }

        return getOrCreateBalance(userId, containerId);
    }

    @Transactional(readOnly = true)
    public List<ClientContainer> getClientContainers(@NonNull Long clientId) {
        return clientContainerRepository.findByClient(clientId);
    }

    private ClientContainer getOrCreateClientContainer(@NonNull Long clientId,
                                                       @NonNull Long userId,
                                                       @NonNull Long containerId,
                                                       @NonNull Container container) {
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

    private ContainerBalance getOrCreateBalance(@NonNull Long userId, @NonNull Long containerId) {
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
