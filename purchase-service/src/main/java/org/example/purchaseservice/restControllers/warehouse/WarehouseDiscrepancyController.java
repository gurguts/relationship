package org.example.purchaseservice.restControllers.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WarehouseDiscrepancyMapper;
import org.example.purchaseservice.models.dto.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.DiscrepancyStatisticsDTO;
import org.example.purchaseservice.models.dto.warehouse.WarehouseDiscrepancyDTO;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.services.impl.IWarehouseDiscrepancyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse/discrepancies")
@RequiredArgsConstructor
@Validated
public class WarehouseDiscrepancyController {
    private final IWarehouseDiscrepancyService discrepancyService;
    private final WarehouseDiscrepancyMapper warehouseDiscrepancyMapper;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping
    public ResponseEntity<PageResponse<WarehouseDiscrepancyDTO>> getDiscrepancies(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) @Positive Long driverId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        Page<WarehouseDiscrepancy> discrepancyPage = discrepancyService.getDiscrepancies(
                driverId, productId, warehouseId, type, dateFrom, dateTo, pageRequest);
        
        Page<WarehouseDiscrepancyDTO> dtoPage = discrepancyPage.map(warehouseDiscrepancyMapper::warehouseDiscrepancyToWarehouseDiscrepancyDTO);
        PageResponse<WarehouseDiscrepancyDTO> response = new PageResponse<>(dtoPage);
        
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/statistics")
    public ResponseEntity<DiscrepancyStatisticsDTO> getStatistics(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        DiscrepancyStatisticsDTO stats = discrepancyService.getStatistics(type, dateFrom, dateTo);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDiscrepancyDTO> getDiscrepancy(@PathVariable @Positive Long id) {
        WarehouseDiscrepancy discrepancy = discrepancyService.getDiscrepancyById(id);
        if (discrepancy == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(warehouseDiscrepancyMapper.warehouseDiscrepancyToWarehouseDiscrepancyDTO(discrepancy));
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) @Positive Long driverId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        try {
            byte[] excelData = discrepancyService.exportToExcel(
                    driverId, productId, warehouseId, type, dateFrom, dateTo
            );
            
            String filename = "vtrati_ta_pridbanna_" + 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename(filename)
                    .build()
            );
            headers.setContentLength(excelData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
                    
        } catch (IOException e) {
            log.error("Error exporting discrepancies to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

