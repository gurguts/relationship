package org.example.containerservice.models.dto.container;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CollectFromClientRequest {
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Client ID is required")
    private Long clientId;
    
    @NotNull(message = "Container ID is required")
    private Long containerId;
    
    @NotNull(message = "Quantity is required")
    private BigDecimal quantity;
}