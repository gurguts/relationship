package org.example.clientservice.models.dto.client;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.List;

public record ClientExportRequest(
        @NonNull
        @NotNull(message = "{validation.fields.notnull}")
        @NotEmpty(message = "{validation.fields.notempty}")
        List<String> fields
) {
}

