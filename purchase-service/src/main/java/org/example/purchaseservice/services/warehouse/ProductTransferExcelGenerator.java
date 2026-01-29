package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.TransferExcelData;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTransferExcelGenerator {
    
    private final ExcelStyleHelper styleHelper;
    
    private static final String EXCEL_SHEET_NAME = "Переміщення";
    private static final String DATE_FORMAT_PATTERN = "dd.MM.yyyy";
    private static final String[] EXCEL_HEADERS = {
            "№", "Дата", "Склад", "З товару", "До товару",
            "Кількість (кг)", "Ціна за кг (грн)", "Загальна вартість (грн)",
            "Користувач ID", "Причина", "Опис"
    };
    private static final int EXCEL_COLUMN_COUNT = EXCEL_HEADERS.length;
    
    public byte[] generateExcel(@NonNull List<TransferExcelData> transfersData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(EXCEL_SHEET_NAME);
            
            CellStyle headerStyle = styleHelper.createHeaderStyle(workbook);
            CellStyle dataStyle = styleHelper.createDataStyle(workbook);
            
            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, transfersData, dataStyle);
            autoSizeColumns(sheet);
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
    
    private void createHeaderRow(@NonNull Sheet sheet, @NonNull CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < EXCEL_HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(EXCEL_HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }
    
    private void fillDataRows(
            @NonNull Sheet sheet,
            @NonNull List<TransferExcelData> transfersData,
            @NonNull CellStyle dataStyle) {
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
        int rowIndex = 1;
        
        for (TransferExcelData data : transfersData) {
            Row row = sheet.createRow(rowIndex);
            fillTransferRow(row, data, rowIndex, dateFormatter, dataStyle);
            rowIndex++;
        }
    }
    
    private void fillTransferRow(
            @NonNull Row row,
            @NonNull TransferExcelData data,
            int rowNumber,
            @NonNull DateTimeFormatter dateFormatter,
            @NonNull CellStyle dataStyle) {
        
        ProductTransferResponseDTO transfer = data.transfer();
        
        createCell(row, 0, rowNumber, dataStyle);
        createCell(row, 1, formatTransferDate(transfer.getTransferDate(), dateFormatter), dataStyle);
        createCell(row, 2, data.warehouseName(), dataStyle);
        createCell(row, 3, data.fromProductName(), dataStyle);
        createCell(row, 4, data.toProductName(), dataStyle);
        createCell(row, 5, getBigDecimalValue(transfer.getQuantity()), dataStyle);
        createCell(row, 6, getBigDecimalValue(transfer.getUnitPriceEur()), dataStyle);
        createCell(row, 7, getBigDecimalValue(transfer.getTotalCostEur()), dataStyle);
        createCell(row, 8, transfer.getUserId() != null ? transfer.getUserId() : "", dataStyle);
        createCell(row, 9, data.reasonName(), dataStyle);
        createCell(row, 10, transfer.getDescription() != null ? transfer.getDescription() : "", dataStyle);
    }
    
    private String formatTransferDate(LocalDate date, @NonNull DateTimeFormatter formatter) {
        return date != null ? date.format(formatter) : "";
    }
    
    private double getBigDecimalValue(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
    
    private void createCell(@NonNull Row row, int columnIndex, Object value, @NonNull CellStyle style) {
        styleHelper.createCell(row, columnIndex, value, style);
    }
    
    private void autoSizeColumns(@NonNull Sheet sheet) {
        for (int i = 0; i < EXCEL_COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
