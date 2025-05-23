package org.example.saleservice.restControllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.saleservice.mappers.SaleMapper;
import org.example.saleservice.models.PageResponse;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.fields.SaleModalDTO;
import org.example.saleservice.models.dto.fields.SalePageDTO;
import org.example.saleservice.services.impl.ISaleSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sale")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SaleSearchController {
    private final ISaleSearchService saleSearchService;
    private final ObjectMapper objectMapper;
    private final SaleMapper saleMapper;

    @PreAuthorize("hasAuthority('sale:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<SalePageDTO>> searchSale(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "direction", defaultValue = "ASC") String direction,
            @RequestParam(name = "filters", required = false) String filters) {

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
            throw new IllegalArgumentException("Invalid filters format");
        }

        PageResponse<SalePageDTO> result = saleSearchService.searchSale(query, pageable, filterParams);

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('sale:view')")
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<SaleModalDTO>> getSalesByClientId(@PathVariable("clientId") Long clientId) {
        List<Sale> saleRecords = saleSearchService.getSalesByClientId(clientId);
        List<SaleModalDTO> saleDTOs = saleRecords.stream()
                .map(saleMapper::saleToSaleModalDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(saleDTOs);
    }
}
