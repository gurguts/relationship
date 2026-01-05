package org.example.purchaseservice.restControllers.purchase;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.example.purchaseservice.services.impl.IPurchaseSpecialOperationsService;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/purchase")
@RequiredArgsConstructor
@Validated
public class PurchaseSpecialOperationsController {
    private final IPurchaseSpecialOperationsService purchaseService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('purchase:excel')")
    @PostMapping("/export/excel")
    public void exportPurchaseToExcel(
            @RequestBody @NonNull Map<String, List<String>> requestBody,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson,
            HttpServletResponse response) {
        try {
            List<String> selectedFields = requestBody.get("fields");

            if (query != null) {
                query = query.trim();
            }

            Map<String, List<String>> filters = new HashMap<>();

            if (filtersJson != null) {
                filters = objectMapper.readValue(filtersJson, new TypeReference<>() {
                });
            }

            purchaseService.generateExcelFile(sortDirection, sortProperty, query, filters, response, selectedFields);
        } catch (Exception e) {
            log.error("Error exporting purchase to Excel", e);
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to export purchase to Excel");
            } catch (Exception ex) {
                log.error("Failed to send error response", ex);
            }
        }
    }

    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping("/report")
    public ResponseEntity<PurchaseReportDTO> generateReportPurchase(
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
            throw new PurchaseException("INVALID_FILTERS", "Invalid filters format");
        }

        PurchaseReportDTO report = purchaseService.generateReport(query, filterParams);

        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasAuthority('purchase:excel')")
    @PostMapping("/comparison/excel")
    public void exportComparisonToExcel(
            @RequestParam(name = "purchaseDataFrom", required = false) String purchaseDataFrom,
            @RequestParam(name = "purchaseDataTo", required = false) String purchaseDataTo,
            HttpServletResponse response) {

        purchaseService.generateComparisonExcelFile(purchaseDataFrom, purchaseDataTo, response);
    }
}
