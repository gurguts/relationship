package org.example.clientservice.services.impl;

import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface IClientSpecialOperationsService {
    byte[] generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            List<String> selectedFields
    );
}
