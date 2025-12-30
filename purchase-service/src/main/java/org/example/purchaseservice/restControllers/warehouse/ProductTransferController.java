package org.example.purchaseservice.restControllers.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.dto.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.example.purchaseservice.services.warehouse.ProductTransferService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class ProductTransferController {
    
    private final ProductTransferService productTransferService;
    private final org.example.purchaseservice.repositories.ProductRepository productRepository;
    private final org.example.purchaseservice.repositories.WarehouseRepository warehouseRepository;
    private final org.example.purchaseservice.repositories.WithdrawalReasonRepository withdrawalReasonRepository;


    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PostMapping("/transfer")
    public ResponseEntity<?> transferProduct(@RequestBody ProductTransferDTO transferDTO) {
        log.info("Received product transfer request: from product {} to product {}, quantity={}", 
                transferDTO.getFromProductId(),
                transferDTO.getToProductId(),
                transferDTO.getQuantity());
        
        try {
            ProductTransfer transfer = productTransferService.transferProduct(transferDTO);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Товар успішно переміщено",
                    "transferId", transfer.getId()
            ));
            
        } catch (RuntimeException e) {
            log.error("Error transferring product", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAuthority('warehouse:view')")
    public ResponseEntity<PageResponse<ProductTransferResponseDTO>> getTransfers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long fromProductId,
            @RequestParam(required = false) Long toProductId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long reasonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transferDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Page<ProductTransferResponseDTO> transfers = productTransferService.getTransfers(
                dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId,
                page, size, sort, direction);

        PageResponse<ProductTransferResponseDTO> response = new PageResponse<>(transfers);
        return ResponseEntity.ok(response);
    }
    
    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PutMapping("/transfers/{id}")
    public ResponseEntity<ProductTransferResponseDTO> updateTransfer(
            @PathVariable Long id,
            @RequestBody ProductTransferUpdateDTO updateDTO) {

        ProductTransferResponseDTO updated = productTransferService.updateTransfer(id, updateDTO);
        if (updated == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/transfers/export")
    @PreAuthorize("hasAuthority('warehouse:view')")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long fromProductId,
            @RequestParam(required = false) Long toProductId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long reasonId) {
        
        try {
            List<ProductTransferResponseDTO> transfers = productTransferService.getAllTransfers(
                    dateFrom, dateTo, warehouseId, fromProductId, toProductId, userId, reasonId);
            
            byte[] excelBytes = generateExcel(transfers);
            
            String filename = "product_transfers_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
            
        } catch (Exception e) {
            log.error("Error exporting transfers to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private byte[] generateExcel(List<ProductTransferResponseDTO> transfers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Переміщення");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"№", "Дата", "Склад", "З товару", "До товару", 
                               "Кількість (кг)", "Ціна за кг (грн)", "Загальна вартість (грн)", 
                               "Користувач ID", "Причина", "Опис"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            int rowNum = 1;
            
            for (ProductTransferResponseDTO transfer : transfers) {
                Row row = sheet.createRow(rowNum);

                String warehouseName = warehouseRepository.findById(transfer.getWarehouseId())
                        .map(w -> w.getName())
                        .orElse("Невідомо");
                String fromProductName = productRepository.findById(transfer.getFromProductId())
                        .map(p -> p.getName())
                        .orElse("Невідомо");
                String toProductName = productRepository.findById(transfer.getToProductId())
                        .map(p -> p.getName())
                        .orElse("Невідомо");
                String reasonName = transfer.getReasonId() != null 
                        ? withdrawalReasonRepository.findById(transfer.getReasonId())
                                .map(r -> r.getName())
                                .orElse("Не вказано")
                        : "Не вказано";

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum);
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(transfer.getTransferDate().format(dateFormatter));
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(warehouseName);
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(fromProductName);
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(toProductName);
                cell4.setCellStyle(dataStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(transfer.getQuantity().doubleValue());
                cell5.setCellStyle(dataStyle);

                Cell cell6 = row.createCell(6);
                cell6.setCellValue(transfer.getUnitPriceEur().doubleValue());
                cell6.setCellStyle(dataStyle);

                Cell cell7 = row.createCell(7);
                cell7.setCellValue(transfer.getTotalCostEur().doubleValue());
                cell7.setCellStyle(dataStyle);

                Cell cell8 = row.createCell(8);
                cell8.setCellValue(transfer.getUserId());
                cell8.setCellStyle(dataStyle);

                Cell cell9 = row.createCell(9);
                cell9.setCellValue(reasonName);
                cell9.setCellStyle(dataStyle);

                Cell cell10 = row.createCell(10);
                cell10.setCellValue(transfer.getDescription() != null ? transfer.getDescription() : "");
                cell10.setCellStyle(dataStyle);
                
                rowNum++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
