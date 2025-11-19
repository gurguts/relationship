package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductTransferUpdateDTO {
    private BigDecimal quantity;
    private Long reasonId;
    private String description;
}

