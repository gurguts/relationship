package org.example.containerservice.services.impl;

import lombok.NonNull;
import org.example.containerservice.models.ContainerBalance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IContainerBalanceService {
    void depositContainer(@NonNull Long userId,
                          @NonNull Long containerId,
                          @NonNull BigDecimal quantity);

    void withdrawContainer(@NonNull Long userId,
                           @NonNull Long containerId,
                           @NonNull BigDecimal quantity);

    @NonNull
    List<ContainerBalance> getUserContainerBalances(@NonNull Long userId);

    @NonNull
    Map<Long, List<ContainerBalance>> getAllUserContainerBalances();
}
