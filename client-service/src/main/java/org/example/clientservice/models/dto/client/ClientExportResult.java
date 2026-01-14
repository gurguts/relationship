package org.example.clientservice.models.dto.client;

import lombok.NonNull;

public record ClientExportResult(
        byte @NonNull [] excelData,
        @NonNull String filename
) {
}

