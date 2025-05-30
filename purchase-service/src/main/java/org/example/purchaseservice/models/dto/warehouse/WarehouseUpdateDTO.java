package org.example.purchaseservice.models.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseUpdateDTO {

    @NotBlank(message = "{validation.business.notblank}")
    @Size(max = 255, message = "{validation.business.size}")
    private String name;

    private String description;
}
