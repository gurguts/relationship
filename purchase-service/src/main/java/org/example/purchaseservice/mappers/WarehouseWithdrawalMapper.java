package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalCreateDTO;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WarehouseWithdrawalMapper {
    @Autowired
    private WithdrawalReasonRepository withdrawalReasonRepository;
    public WarehouseWithdrawal withdrawalCreateDTOToWarehouseWithdrawal(WithdrawalCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        WarehouseWithdrawal warehouseWithdrawal = new WarehouseWithdrawal();
        warehouseWithdrawal.setWarehouseId(dto.getWarehouseId());
        warehouseWithdrawal.setProductId(dto.getProductId());
        warehouseWithdrawal.setWarehouseId(dto.getWarehouseId());
        
        WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(dto.getWithdrawalReasonId())
                .orElseThrow(() -> new RuntimeException("WithdrawalReason not found with id: " + dto.getWithdrawalReasonId()));
        warehouseWithdrawal.setWithdrawalReason(withdrawalReason);
        
        warehouseWithdrawal.setQuantity(BigDecimal.valueOf(dto.getQuantity()));
        warehouseWithdrawal.setDescription(dto.getDescription());
        // withdrawalDate will be set from createdAt in service

        return warehouseWithdrawal;
    }

    public WarehouseWithdrawalDTO warehouseWithdrawalToWarehouseWithdrawalDTO(WarehouseWithdrawal warehouseWithdrawal) {
        WarehouseWithdrawalDTO dto = new WarehouseWithdrawalDTO();

        dto.setId(warehouseWithdrawal.getId());
        dto.setWarehouseId(warehouseWithdrawal.getWarehouseId());
        dto.setProductId(warehouseWithdrawal.getProductId());
        dto.setUserId(warehouseWithdrawal.getUserId());
        dto.setWarehouseId(warehouseWithdrawal.getWarehouseId());
        dto.setWithdrawalReason(warehouseWithdrawal.getWithdrawalReason());
        dto.setQuantity(warehouseWithdrawal.getQuantity());
        dto.setDescription(warehouseWithdrawal.getDescription());
        dto.setWithdrawalDate(warehouseWithdrawal.getWithdrawalDate());
        dto.setCreatedAt(warehouseWithdrawal.getCreatedAt());

        return dto;
    }

    public WarehouseWithdrawal withdrawalUpdateDTOToWarehouseWithdrawal(WarehouseWithdrawalUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        WarehouseWithdrawal warehouseWithdrawal = new WarehouseWithdrawal();
        
        if (dto.getProductId() != null) {
            warehouseWithdrawal.setProductId(dto.getProductId());
        }
        
        if (dto.getWithdrawalReasonId() != null) {
            WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(dto.getWithdrawalReasonId())
                    .orElseThrow(() -> new RuntimeException("WithdrawalReason not found with id: " + dto.getWithdrawalReasonId()));
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
