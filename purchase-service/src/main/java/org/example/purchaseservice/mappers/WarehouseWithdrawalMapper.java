package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalCreateDTO;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class WarehouseWithdrawalMapper {
    public WarehouseWithdrawal withdrawalCreateDTOToWarehouseWithdrawal(WithdrawalCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        WarehouseWithdrawal warehouseWithdrawal = new WarehouseWithdrawal();
        warehouseWithdrawal.setWarehouseId(dto.getWarehouseId());
        warehouseWithdrawal.setProductId(dto.getProductId());
        warehouseWithdrawal.setWarehouseId(dto.getWarehouseId());
        warehouseWithdrawal.setReasonType(WithdrawalReason.valueOf(dto.getReasonType()));
        warehouseWithdrawal.setQuantity(BigDecimal.valueOf(dto.getQuantity()));
        warehouseWithdrawal.setDescription(dto.getDescription());
        warehouseWithdrawal.setWithdrawalDate(dto.getWithdrawalDate());

        return warehouseWithdrawal;
    }

    public WarehouseWithdrawalDTO warehouseWithdrawalToWarehouseWithdrawalDTO(WarehouseWithdrawal warehouseWithdrawal) {
        WarehouseWithdrawalDTO dto = new WarehouseWithdrawalDTO();

        dto.setId(warehouseWithdrawal.getId());
        dto.setWarehouseId(warehouseWithdrawal.getWarehouseId());
        dto.setProductId(warehouseWithdrawal.getProductId());
        dto.setUserId(warehouseWithdrawal.getUserId());
        dto.setWarehouseId(warehouseWithdrawal.getWarehouseId());
        dto.setReasonType(warehouseWithdrawal.getReasonType());
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
        warehouseWithdrawal.setReasonType(WithdrawalReason.valueOf(dto.getReasonType()));
        warehouseWithdrawal.setQuantity(BigDecimal.valueOf(dto.getQuantity()));
        warehouseWithdrawal.setDescription(dto.getDescription());
        warehouseWithdrawal.setWithdrawalDate(dto.getWithdrawalDate());

        return warehouseWithdrawal;
    }
}
