package org.example.containerservice.models.dto.container;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContainerOperationRequest {
    private Long userId;
    private Long containerId;
    private BigDecimal quantity;
}
