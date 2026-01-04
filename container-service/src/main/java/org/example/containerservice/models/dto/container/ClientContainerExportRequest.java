package org.example.containerservice.models.dto.container;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.List;

public record ClientContainerExportRequest(
        @NonNull
        @NotNull(message = "{validation.fields.notnull}")
        @NotEmpty(message = "{validation.fields.notempty}")
        List<String> fields
) {
}

