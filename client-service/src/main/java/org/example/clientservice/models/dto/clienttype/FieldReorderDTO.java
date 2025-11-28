package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FieldReorderDTO {
    @NotEmpty
    private List<Long> fieldIds;
}

