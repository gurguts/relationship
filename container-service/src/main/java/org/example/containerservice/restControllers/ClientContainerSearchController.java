package org.example.containerservice.restControllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.dto.PageResponse;
import org.example.containerservice.models.dto.container.ClientContainerResponseDTO;
import org.example.containerservice.services.impl.IClientContainerSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/containers/client")
@RequiredArgsConstructor
@Validated
public class ClientContainerSearchController {
    private final ObjectMapper objectMapper;
    private final IClientContainerSearchService clientContainerService;

    @PreAuthorize("hasAuthority('container:view')")
    @GetMapping("/search-containers")
    public ResponseEntity<PageResponse<ClientContainerResponseDTO>> searchClientContainers(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(name = "size", defaultValue = "100") @Positive @Max(1000) int size,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson) {

        String validatedSortProperty = validateSortProperty(sortProperty);
        Sort sortBy = Sort.by(sortDirection, validatedSortProperty);
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Map<String, List<String>> filterParams = parseFilters(filtersJson);

        PageResponse<ClientContainerResponseDTO> result = clientContainerService.searchClientContainer(
                query, pageable, filterParams);

        return ResponseEntity.ok(result);
    }

    private String validateSortProperty(String sortProperty) {
        if (sortProperty == null || sortProperty.trim().isEmpty()) {
            return "updatedAt";
        }
        String[] validSortFields = {"quantity", "updatedAt"};
        for (String validField : validSortFields) {
            if (validField.equals(sortProperty)) {
                return sortProperty;
            }
        }
        return "updatedAt";
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
