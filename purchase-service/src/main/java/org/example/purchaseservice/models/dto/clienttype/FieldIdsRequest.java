package org.example.purchaseservice.models.dto.clienttype;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

import java.util.List;

public record FieldIdsRequest(
        @NonNull
        @NotNull
        @NotEmpty
        @Size(max = 100)
        List<@Positive Long> fieldIds
) {
}
