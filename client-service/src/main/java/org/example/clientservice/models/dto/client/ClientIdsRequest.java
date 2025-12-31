package org.example.clientservice.models.dto.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.NonNull;

import java.util.List;

public record ClientIdsRequest(
        @NonNull
        @NotNull(message = "{validation.clientIds.notnull}")
        @NotEmpty(message = "{validation.clientIds.notempty}")
        @Size(max = 100, message = "{validation.clientIds.size}")
        @Valid
        List<@Positive(message = "{validation.clientId.positive}") Long> clientIds
) {
}

