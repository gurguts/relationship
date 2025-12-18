package org.example.containerservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.exceptions.ContainerNotFoundException;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.models.ContainerTransactionType;
import org.example.containerservice.repositories.ContainerBalanceRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContainerBalanceService {

    private final ContainerBalanceRepository containerBalanceRepository;
    private final ContainerTransactionService containerTransactionService;
    private final IContainerService containerService;

    @Transactional
    public void depositContainer(Long userId, Long containerId, BigDecimal quantity) {
        validateParameters(userId, containerId, quantity);

        ContainerBalance balance = getOrCreateBalance(userId, containerId);
        balance.setTotalQuantity(balance.getTotalQuantity().add(quantity));
        containerBalanceRepository.save(balance);

        org.example.containerservice.models.Container container = balance.getContainer();
        if (container == null) {
            throw new ContainerException("INVALID_BALANCE", "Container is null in balance");
        }

        containerTransactionService.logTransaction(userId, null, null, container, quantity,
                ContainerTransactionType.DEPOSIT);
        log.info("Deposited {} units of container {} for user {}", quantity, containerId, userId);
    }

    @Transactional
    public void withdrawContainer(Long userId, Long containerId, BigDecimal quantity) {
        validateParameters(userId, containerId, quantity);

        ContainerBalance balance = containerBalanceRepository.findByUserIdAndContainerId(userId, containerId)
                .orElseThrow(() -> new ContainerNotFoundException(
                        String.format("No balance found for user %d and container %s", userId, containerId)));

        BigDecimal newTotal = balance.getTotalQuantity().subtract(quantity);

        if (newTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ContainerException("NOT_ENOUGH_CONTAINERS", "Cannot withdraw more than available free containers");
        }

        balance.setTotalQuantity(newTotal);
        containerBalanceRepository.save(balance);

        org.example.containerservice.models.Container container = balance.getContainer();
        if (container == null) {
            throw new ContainerException("INVALID_BALANCE", "Container is null in balance");
        }

        containerTransactionService.logTransaction(userId, null, null, container, quantity,
                ContainerTransactionType.WITHDRAW);
        log.info("Withdrew {} units of container {} from user {}", quantity, containerId, userId);
    }

    private void validateParameters(Long userId, Long containerId, BigDecimal quantity) {
        if (userId == null || containerId == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ContainerException("NOT_ENOUGH_DATA", "User ID, container ID, and quantity must be valid");
        }
    }

    @Transactional(readOnly = true)
    public List<ContainerBalance> getUserContainerBalances(Long userId) {
        return containerBalanceRepository.findByUserId(userId);
    }

    protected ContainerBalance getOrCreateBalance(Long userId, Long containerId) {
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

    @Transactional(readOnly = true)
    public Map<Long, List<ContainerBalance>> getAllUserContainerBalances() {
        List<ContainerBalance> balances = containerBalanceRepository.findAllOrderedByUserId();
        return balances.stream()
                .collect(Collectors.groupingBy(ContainerBalance::getUserId));
    }
}
