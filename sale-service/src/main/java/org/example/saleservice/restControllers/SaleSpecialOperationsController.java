package org.example.saleservice.restControllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saleservice.models.dto.fields.SaleReportDTO;
import org.example.saleservice.services.impl.ISaleSpecialOperationsService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sale")
@RequiredArgsConstructor
@Slf4j
public class SaleSpecialOperationsController {
    private final ISaleSpecialOperationsService saleService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('sale:excel')")
    @PostMapping("/export/excel")
    public void exportSaleToExcel(
            @RequestBody Map<String, List<String>> requestBody,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson,
            HttpServletResponse response) throws Exception {

        List<String> selectedFields = requestBody.get("fields");

        if (query != null) {
            query = query.trim();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, List<String>> filters = new HashMap<>();

        if (filtersJson != null) {
            filters = objectMapper.readValue(filtersJson, new TypeReference<>() {
            });
        }

        saleService.generateExcelFile(sortDirection, sortProperty, query, filters, response, selectedFields);
    }

    @PreAuthorize("hasAuthority('sale:view')")
    @GetMapping("/report")
    public ResponseEntity<SaleReportDTO> generateReportPurchase(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "filters", required = false) String filters) {

        Map<String, List<String>> filterParams;

        try {
            if (filters != null && !filters.isEmpty()) {
                filterParams = objectMapper.readValue(filters, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, List.class));
            } else {
                filterParams = Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Failed to parse filters: {}", filters, e);
            throw new IllegalArgumentException("Invalid filters format");
        }

        SaleReportDTO report = saleService.generateReport(query, filterParams);

        return ResponseEntity.ok(report);
    }
}
