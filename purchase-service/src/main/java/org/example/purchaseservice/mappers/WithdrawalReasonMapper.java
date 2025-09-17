package org.example.purchaseservice.mappers;

import org.example.purchaseservice.models.dto.warehouse.WithdrawalReasonCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalReasonDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalReasonUpdateDTO;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalReasonMapper {
    public WithdrawalReasonDTO withdrawalReasonToWithdrawalReasonDTO(WithdrawalReason withdrawalReason) {
        if (withdrawalReason == null) {
            return null;
        }
        WithdrawalReasonDTO dto = new WithdrawalReasonDTO();
        dto.setId(withdrawalReason.getId());
        dto.setName(withdrawalReason.getName());
        dto.setPurpose(withdrawalReason.getPurpose());
        return dto;
    }

    public WithdrawalReason withdrawalReasonCreateDTOToWithdrawalReason(WithdrawalReasonCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        WithdrawalReason withdrawalReason = new WithdrawalReason();
        withdrawalReason.setName(dto.getName());
        withdrawalReason.setPurpose(dto.getPurpose());
        return withdrawalReason;
    }

    public WithdrawalReason withdrawalReasonUpdateDTOToWithdrawalReason(WithdrawalReasonUpdateDTO dto) {
        if (dto == null) {
            return null;
        }
        WithdrawalReason withdrawalReason = new WithdrawalReason();
        withdrawalReason.setName(dto.getName());
        withdrawalReason.setPurpose(dto.getPurpose());
        return withdrawalReason;
    }
}
