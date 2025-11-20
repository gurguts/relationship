package org.example.purchaseservice.models.dto.balance;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseBalanceUpdateRequest {
    private BigDecimal newQuantity;
    private BigDecimal newTotalCostEur;
    private String description;
}
