package org.example.clientservice.services.impl;

import org.springframework.web.multipart.MultipartFile;

public interface IClientImportService {
    byte[] generateTemplate(Long clientTypeId);
    String importClients(Long clientTypeId, MultipartFile file);
}

