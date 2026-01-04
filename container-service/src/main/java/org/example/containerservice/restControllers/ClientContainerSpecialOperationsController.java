package org.example.containerservice.restControllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.dto.container.ClientContainerExportRequest;
import org.example.containerservice.services.impl.IClientContainerSpecialOperationsService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/containers")
@RequiredArgsConstructor
@Validated
public class ClientContainerSpecialOperationsController {
    private final IClientContainerSpecialOperationsService clientContainerService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('container:excel')")
    @PostMapping("/export/excel")
    public void exportClientContainerToExcel(
            @RequestBody @Valid @NonNull ClientContainerExportRequest request,
            @RequestParam(name = "q", required = false)
            @Size(max = 255, message = "{validation.query.size}") String query,
            @RequestParam(name = "sort", defaultValue = "updatedAt")
            @Pattern(regexp = "^(quantity|updatedAt)$", message = "{validation.sort.property}")
            String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson,
            HttpServletResponse response) {

        String normalizedQuery = query != null ? query.trim() : null;
        Map<String, List<String>> filters = parseFilters(filtersJson);

        clientContainerService.generateExcelFile(
                sortDirection, sortProperty, normalizedQuery, filters, response, request.fields());
    }

    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ContainerException("INVALID_JSON", "Invalid JSON format for filters");
        }
    }
}
