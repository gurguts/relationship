package org.example.containerservice.restControllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.dto.container.ClientContainerResponseDTO;
import org.example.containerservice.models.dto.PageResponse;
import org.example.containerservice.services.ClientContainerSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/containers/client")
@RequiredArgsConstructor
public class ClientContainerSearchController {
    private final ObjectMapper objectMapper;
    private final ClientContainerSearchService clientContainerService;

    @PreAuthorize("hasAuthority('container:view')")
    @GetMapping("/search-containers")
    public ResponseEntity<PageResponse<ClientContainerResponseDTO>> searchClientContainers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "updatedAt") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String filters) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        Pageable pageable = PageRequest.of(page, size, sortBy);

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
            throw new ContainerException("NOT_VALID_FILTERS", "Invalid filters format");
        }

        PageResponse<ClientContainerResponseDTO> result = clientContainerService.searchClientContainer(query, pageable,
                filterParams);

        return ResponseEntity.ok(result);
    }
}
