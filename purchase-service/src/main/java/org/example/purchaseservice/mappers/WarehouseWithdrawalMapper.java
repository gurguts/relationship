package org.example.purchaseservice.mappers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.WithdrawalReasonNotFoundException;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalCreateDTO;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class WarehouseWithdrawalMapper {

    private final WithdrawalReasonRepository withdrawalReasonRepository;

    public WarehouseWithdrawal withdrawalCreateDTOToWarehouseWithdrawal(@NonNull WithdrawalCreateDTO dto) {
        WarehouseWithdrawal warehouseWithdrawal = new WarehouseWithdrawal();
        warehouseWithdrawal.setWarehouseId(dto.getWarehouseId());
        warehouseWithdrawal.setProductId(dto.getProductId());
        
        WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(dto.getWithdrawalReasonId())
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                        "WithdrawalReason not found with id: " + dto.getWithdrawalReasonId()));
        warehouseWithdrawal.setWithdrawalReason(withdrawalReason);
        
        warehouseWithdrawal.setQuantity(BigDecimal.valueOf(dto.getQuantity()));
        warehouseWithdrawal.setDescription(dto.getDescription());
        return warehouseWithdrawal;
    }

    public WarehouseWithdrawalDTO warehouseWithdrawalToWarehouseWithdrawalDTO(@NonNull WarehouseWithdrawal warehouseWithdrawal) {
        WarehouseWithdrawalDTO dto = new WarehouseWithdrawalDTO();
        dto.setId(warehouseWithdrawal.getId());
        dto.setWarehouseId(warehouseWithdrawal.getWarehouseId());
        dto.setProductId(warehouseWithdrawal.getProductId());
        dto.setUserId(warehouseWithdrawal.getUserId());
        dto.setWithdrawalReason(warehouseWithdrawal.getWithdrawalReason());
        dto.setQuantity(warehouseWithdrawal.getQuantity());
        dto.setDescription(warehouseWithdrawal.getDescription());
        dto.setWithdrawalDate(warehouseWithdrawal.getWithdrawalDate());
        dto.setCreatedAt(warehouseWithdrawal.getCreatedAt());
        return dto;
    }

    public WarehouseWithdrawal withdrawalUpdateDTOToWarehouseWithdrawal(@NonNull WarehouseWithdrawalUpdateDTO dto) {
        WarehouseWithdrawal warehouseWithdrawal = new WarehouseWithdrawal();
        
        if (dto.getProductId() != null) {
            warehouseWithdrawal.setProductId(dto.getProductId());
        }
        
        if (dto.getWithdrawalReasonId() != null) {
            WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(dto.getWithdrawalReasonId())
                    .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                            "WithdrawalReason not found with id: " + dto.getWithdrawalReasonId()));
            warehouseWithdrawal.setWithdrawalReason(withdrawalReason);
        }
        
        if (dto.getQuantity() != null) {
            warehouseWithdrawal.setQuantity(BigDecimal.valueOf(dto.getQuantity()));
        }
        if (dto.getDescription() != null) {
            warehouseWithdrawal.setDescription(dto.getDescription());
        }
        if (dto.getWithdrawalDate() != null) {
            warehouseWithdrawal.setWithdrawalDate(dto.getWithdrawalDate());
        }
        return warehouseWithdrawal;
    }
}
