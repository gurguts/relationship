package org.example.purchaseservice.models.dto.warehouse;

import lombok.Data;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;

@Data
public class WithdrawalReasonDTO {
    private Long id;
    private String name;
    private WithdrawalReason.Purpose purpose;
}
