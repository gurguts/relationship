package org.example.clientservice.models.dto.clienttype;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

import java.util.List;

public record FieldIdsRequest(
        @NonNull
        @NotNull(message = "{validation.fieldIds.notnull}")
        @NotEmpty(message = "{validation.fieldIds.notempty}")
        @Size(max = 100, message = "{validation.fieldIds.size}")
        @Valid
        List<@Positive(message = "{validation.fieldId.positive}") Long> fieldIds
) {
}

