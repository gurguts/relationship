package org.example.clientservice.services.impl;

import lombok.NonNull;
import org.example.clientservice.models.dto.client.ClientExportResult;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface IClientSpecialOperationsService {
    @NonNull
    ClientExportResult exportClientsToExcel(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            List<String> selectedFields
    );
}
