package org.example.containerservice.models.dto.container;

import lombok.Data;
import org.example.containerservice.models.ContainerTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ContainerTransactionDTO {
    private Long fromUserId;
    private Long toUserId;
    private Long clientId;
    private Long containerId;
    private String containerName;
    private BigDecimal quantity;
    private ContainerTransactionType type;
    private LocalDateTime createdAt;
}
