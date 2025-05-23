package org.example.containerservice.models.dto.container;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClientContainerDTO {
    private Long clientId;
    private Long userId;
    private Long containerId;
    private String containerName;
    private BigDecimal quantity;
    private LocalDateTime updatedAt;
}
