package org.example.containerservice.restControllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.services.impl.IClientContainerSpecialOperationsService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/containers")
@RequiredArgsConstructor
@Slf4j
public class ClientContainerSpecialOperationsController {
    private final IClientContainerSpecialOperationsService clientContainerService;

    @PreAuthorize("hasAuthority('container:excel')")
    @PostMapping("/export/excel")
    public void exportClientContainerToExcel(
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

        clientContainerService.generateExcelFile(sortDirection, sortProperty, query, filters, response, selectedFields);
    }
}
