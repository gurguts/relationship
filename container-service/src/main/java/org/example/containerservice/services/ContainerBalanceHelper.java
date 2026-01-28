package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.containerservice.models.ContainerBalance;
import org.example.containerservice.repositories.ContainerBalanceRepository;
import org.example.containerservice.services.impl.IContainerService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ContainerBalanceHelper {

    private final ContainerBalanceRepository containerBalanceRepository;
    private final IContainerService containerService;

    public ContainerBalance getOrCreateBalance(@NonNull Long userId, @NonNull Long containerId) {
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
