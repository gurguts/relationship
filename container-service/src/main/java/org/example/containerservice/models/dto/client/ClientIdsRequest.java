package org.example.containerservice.models.dto.client;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

import java.util.List;

public record ClientIdsRequest(
        @NonNull
        @NotNull
        @NotEmpty
        @Size(max = 100)
        List<@Positive Long> clientIds
) {
}
