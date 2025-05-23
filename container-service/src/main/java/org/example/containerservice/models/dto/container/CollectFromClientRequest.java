package org.example.containerservice.models.dto.container;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CollectFromClientRequest {
    private Long clientId;
    private Long containerId;
    private BigDecimal quantity;
}