package org.example.clientservice.models.dto.client;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ClientSearchRequest(
        @Size(max = 255, message = "{validation.query.size}")
        String query,
        Map<String, List<String>> filterParams,
        @Positive(message = "{validation.clientTypeId.positive}")
        Long clientTypeId
) {
}
