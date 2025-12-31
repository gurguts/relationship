package org.example.clientservice.models.dto.client;

import lombok.NonNull;

public record ClientExportResult(
        @NonNull byte[] excelData,
        @NonNull String filename
) {
}

