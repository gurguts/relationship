package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

public interface IClientImportService {
    byte[] generateTemplate(@NonNull Long clientTypeId);
    String importClients(@NonNull Long clientTypeId, @NonNull MultipartFile file);
}

