package org.example.clientservice.models.client;

import org.example.clientservice.models.field.Source;

import java.util.List;

public record FilterIds(
        List<Source> sourceDTOs, List<Long> sourceIds
) {
}