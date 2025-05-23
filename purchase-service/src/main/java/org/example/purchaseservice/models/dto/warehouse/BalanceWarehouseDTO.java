package org.example.purchaseservice.models.dto.warehouse;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BalanceWarehouseDTO {
    private Map<Long, Double> balanceByProduct;
}
