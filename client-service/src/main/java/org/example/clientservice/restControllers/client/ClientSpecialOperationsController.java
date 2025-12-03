package org.example.clientservice.restControllers.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.services.impl.IClientSpecialOperationsService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Slf4j
public class ClientSpecialOperationsController {
    private final IClientSpecialOperationsService clientService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('client:excel')")
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportClientToExcel(
            @RequestBody Map<String, List<String>> requestBody,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson
    ) {
        List<String> selectedFields = requestBody.get("fields");
        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }

        if (query != null) {
            query = query.trim();
        }

        Map<String, List<String>> filters = parseFilters(filtersJson);

        byte[] excelData = clientService.generateExcelFile(
                sortDirection, sortProperty, query, filters, selectedFields);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=client_data.xlsx");

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ClientException("INVALID_JSON", String.format("Invalid JSON format for filters: %s",
                    e.getMessage()));
        }
    }
}
