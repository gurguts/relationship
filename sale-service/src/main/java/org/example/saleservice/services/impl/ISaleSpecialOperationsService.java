package org.example.saleservice.services.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.example.saleservice.models.dto.fields.SaleReportDTO;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ISaleSpecialOperationsService {

    void generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            HttpServletResponse response,
            List<String> selectedFields) throws IOException;

    SaleReportDTO generateReport(String query, Map<String, List<String>> filterParams);
}
