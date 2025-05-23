package org.example.containerservice.services.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IClientContainerSpecialOperationsService {

    void generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            HttpServletResponse response,
            List<String> selectedFields) throws IOException;
}
