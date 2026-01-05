package org.example.purchaseservice.restControllers.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.dto.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.services.warehouse.ProductTransferService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Validated
public class ProductTransferController {
    private final ProductTransferService productTransferService;

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PostMapping("/transfer")
    public ResponseEntity<ProductTransferResponseDTO> transferProduct(@RequestBody @Valid @NonNull ProductTransferDTO transferDTO) {
        log.info("Received product transfer request: from product {} to product {}, quantity={}", 
                transferDTO.getFromProductId(),
                transferDTO.getToProductId(),
                transferDTO.getQuantity());
        
        org.example.purchaseservice.models.warehouse.ProductTransfer transfer = productTransferService.transferProduct(transferDTO);
        ProductTransferResponseDTO responseDTO = productTransferService.toDTO(transfer);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/transfers/{id}")
                .buildAndExpand(transfer.getId())
                .toUri();
        
        return ResponseEntity.created(location).body(responseDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/transfers")
    public ResponseEntity<PageResponse<ProductTransferResponseDTO>> getTransfers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long fromProductId,
            @RequestParam(required = false) @Positive Long toProductId,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long reasonId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "transferDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Page<ProductTransferResponseDTO> transfers = productTransferService.getTransfers(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId,
                page, size, sort, direction);

        PageResponse<ProductTransferResponseDTO> response = new PageResponse<>(transfers);
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PatchMapping("/transfers/{id}")
    public ResponseEntity<ProductTransferResponseDTO> updateTransfer(
            @PathVariable @Positive Long id,
            @RequestBody @Valid @NonNull ProductTransferUpdateDTO updateDTO) {
        ProductTransferResponseDTO updated = productTransferService.updateTransfer(id, updateDTO);
        return updated != null 
                ? ResponseEntity.ok(updated) 
                : ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/transfers/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long fromProductId,
            @RequestParam(required = false) @Positive Long toProductId,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long reasonId) {
        
        try {
            byte[] excelBytes = productTransferService.exportToExcel(
                    dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
            
            String filename = "product_transfers_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
            
        } catch (IOException e) {
            log.error("Error exporting transfers to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
