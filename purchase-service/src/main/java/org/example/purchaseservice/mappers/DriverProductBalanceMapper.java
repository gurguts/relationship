package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.DriverProductBalance;
import org.example.purchaseservice.models.dto.balance.DriverProductBalanceDTO;
import org.springframework.stereotype.Component;

@Component
public class DriverProductBalanceMapper {

    public DriverProductBalanceDTO driverProductBalanceToDTO(@NonNull DriverProductBalance balance) {
        return DriverProductBalanceDTO.builder()
                .id(balance.getId())
                .driverId(balance.getDriverId())
                .productId(balance.getProductId())
                .quantity(balance.getQuantity())
                .averagePriceEur(balance.getAveragePriceEur())
                .totalCostEur(balance.getTotalCostEur())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }
}
