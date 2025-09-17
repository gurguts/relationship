package org.example.purchaseservice.models.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;

@Data
public class WithdrawalReasonCreateDTO {
    @NotBlank(message = "{validation.withdrawalReason.name.notblank}")
    @Size(max = 255, message = "{validation.withdrawalReason.name.size}")
    private String name;

    @NotNull(message = "{validation.withdrawalReason.purpose.notnull}")
    private WithdrawalReason.Purpose purpose;
}
