package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.exceptions.ContainerNotFoundException;
import org.example.containerservice.models.Container;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ContainerBalanceRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.example.containerservice.services.impl.IContainerBalanceService;
import org.example.containerservice.services.impl.IContainerTransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerBalanceService implements IContainerBalanceService {

    private static final String ERROR_INVALID_BALANCE = "INVALID_BALANCE";
    private static final String ERROR_NOT_ENOUGH_DATA = "NOT_ENOUGH_DATA";
    private static final String ERROR_NOT_ENOUGH_CONTAINERS = "NOT_ENOUGH_CONTAINERS";
    private static final String MESSAGE_CONTAINER_NULL = "Container is null in balance";
    private static final String MESSAGE_VALIDATION_REQUIRED = "User ID, container ID, and quantity must be valid";
    private static final String MESSAGE_NOT_ENOUGH_CONTAINERS = "Cannot withdraw more than available free containers";
    private static final String MESSAGE_BALANCE_NOT_FOUND = "No balance found for user %d and container %d";

    private final ContainerBalanceRepository containerBalanceRepository;
    private final IContainerTransactionService containerTransactionService;
    private final IContainerService containerService;

    @Override
    @Transactional
    public void depositContainer(@NonNull Long userId,
                                 @NonNull Long containerId,
                                 @NonNull BigDecimal quantity) {
        validateParameters(quantity);

        ContainerBalance balance = getOrCreateBalance(userId, containerId);
        balance.setTotalQuantity(balance.getTotalQuantity().add(quantity));
        containerBalanceRepository.save(balance);

        Container container = balance.getContainer();
        if (container == null) {
            throw new ContainerException(ERROR_INVALID_BALANCE, MESSAGE_CONTAINER_NULL);
        }

        containerTransactionService.logTransaction(userId, null, null, container, quantity,
                ContainerTransactionType.DEPOSIT);
        log.info("Deposited {} units of container {} for user {}", quantity, containerId, userId);
    }

    @Override
    @Transactional
    public void withdrawContainer(@NonNull Long userId,
                                  @NonNull Long containerId,
                                  @NonNull BigDecimal quantity) {
        validateParameters(quantity);

        ContainerBalance balance = containerBalanceRepository.findByUserIdAndContainerId(userId, containerId)
                .orElseThrow(() -> new ContainerNotFoundException(
                        String.format(MESSAGE_BALANCE_NOT_FOUND, userId, containerId)));

        BigDecimal newTotal = balance.getTotalQuantity().subtract(quantity);

        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ContainerException(ERROR_NOT_ENOUGH_CONTAINERS, MESSAGE_NOT_ENOUGH_CONTAINERS);
        }

        balance.setTotalQuantity(newTotal);
        containerBalanceRepository.save(balance);

        Container container = balance.getContainer();
        if (container == null) {
            throw new ContainerException(ERROR_INVALID_BALANCE, MESSAGE_CONTAINER_NULL);
        }

        containerTransactionService.logTransaction(userId, null, null, container, quantity,
                ContainerTransactionType.WITHDRAW);
        log.info("Withdrew {} units of container {} from user {}", quantity, containerId, userId);
    }

    private void validateParameters(@NonNull BigDecimal quantity) {
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ContainerException(ERROR_NOT_ENOUGH_DATA, MESSAGE_VALIDATION_REQUIRED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @NonNull
    public List<ContainerBalance> getUserContainerBalances(@NonNull Long userId) {
        return containerBalanceRepository.findByUserId(userId);
    }

    protected ContainerBalance getOrCreateBalance(@NonNull Long userId, @NonNull Long containerId) {
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

    @Override
    @Transactional(readOnly = true)
    @NonNull
    public Map<Long, List<ContainerBalance>> getAllUserContainerBalances() {
        List<ContainerBalance> balances = containerBalanceRepository.findAllOrderedByUserId();
        return balances.stream()
                .collect(Collectors.groupingBy(ContainerBalance::getUserId));
    }
}
