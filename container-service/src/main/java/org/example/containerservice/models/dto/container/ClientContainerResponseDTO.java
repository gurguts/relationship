package org.example.containerservice.models.dto.container;

import lombok.Data;
import org.example.containerservice.models.dto.client.ClientDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClientContainerResponseDTO {
    private Long userId;
    private Long containerId;
    private String containerName;
    private BigDecimal quantity;
    private LocalDateTime updatedAt;
    private ClientDTO client;
}
