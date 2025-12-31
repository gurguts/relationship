package org.example.clientservice.models.client;

import lombok.NonNull;
import org.example.clientservice.models.field.Source;

import java.util.List;

public record ClientFilterIds(
        @NonNull List<Source> sourceDTOs,
        @NonNull List<Long> sourceIds
) {
}

