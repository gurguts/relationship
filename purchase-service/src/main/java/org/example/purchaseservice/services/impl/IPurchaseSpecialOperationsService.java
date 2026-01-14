package org.example.purchaseservice.services.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public interface IPurchaseSpecialOperationsService {

    void generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            HttpServletResponse response,
            List<String> selectedFields);

    void generateComparisonExcelFile(String purchaseDataFrom, String purchaseDataTo, HttpServletResponse response);
}
