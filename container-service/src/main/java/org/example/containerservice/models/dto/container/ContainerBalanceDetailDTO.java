package org.example.containerservice.models.dto.container;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContainerBalanceDetailDTO {
    private Long containerId;
    private String containerName;
    private BigDecimal totalQuantity;
    private BigDecimal clientQuantity;
}