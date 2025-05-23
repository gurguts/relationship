package org.example.warehouseservice.restControllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.warehouseservice.models.dto.PageResponse;
import org.example.warehouseservice.models.dto.WarehouseEntryDTO;
import org.example.warehouseservice.services.WarehouseEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseEntryController {
    private final WarehouseEntryService warehouseEntryService;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/entries")
    public ResponseEntity<PageResponse<WarehouseEntryDTO>> getWarehouseEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "entryDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {
        Map<String, List<String>> filterMap = filters != null && !filters.isEmpty()
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {})
                : Collections.emptyMap();

        PageResponse<WarehouseEntryDTO> result = warehouseEntryService.getWarehouseEntries(page, size, sort, direction, filterMap);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('warehouse:create')")
    @PostMapping("/entries")
    public ResponseEntity<WarehouseEntryDTO> createWarehouseEntry(@RequestBody WarehouseEntryDTO dto) {
        WarehouseEntryDTO result = warehouseEntryService.createWarehouseEntry(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}


