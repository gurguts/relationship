package org.example.containerservice.services.impl;

import lombok.NonNull;
import org.example.containerservice.models.ClientContainer;

import java.math.BigDecimal;
import java.util.List;

public interface IClientContainerService {
    void transferContainerToClient(@NonNull Long userId,
                                    @NonNull Long clientId,
                                    @NonNull Long containerId,
                                    @NonNull BigDecimal quantity);

    void collectContainerFromClient(@NonNull Long userId,
                                     @NonNull Long clientId,
                                     @NonNull Long containerId,
                                     @NonNull BigDecimal quantity);

    @NonNull
    List<ClientContainer> getClientContainers(@NonNull Long clientId);
}
