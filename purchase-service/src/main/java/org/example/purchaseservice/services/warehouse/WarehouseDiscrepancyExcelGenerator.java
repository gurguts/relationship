package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.warehouse.WarehouseDiscrepancy;
import org.example.purchaseservice.services.warehouse.WarehouseDiscrepancyExcelDataFetcher.ExcelData;
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
public class WarehouseDiscrepancyExcelGenerator {
    
    private static final String EXCEL_SHEET_NAME = "Втрати та придбання";
    private static final String DATE_FORMAT_PATTERN = "dd.MM.yyyy";
    private static final String TYPE_LOSS = "ВТРАТА";
    private static final String TYPE_GAIN = "ПРИДБАННЯ";
    
    private static final String[] EXCEL_HEADERS = {
            "№", "Дата", "Водій ID", "Товар", "Склад",
            "Закуплено (кг)", "Прийнято (кг)", "Різниця (кг)",
            "Ціна за кг (грн)", "Вартість різниці (грн)", "Тип", "Коментар"
    };
    
    private static final int EXCEL_COLUMN_COUNT = EXCEL_HEADERS.length;
    
    private final WarehouseDiscrepancyExcelDataFetcher dataFetcher;
    private final ExcelStyleHelper styleHelper;
    
    public byte[] generateExcel(@NonNull List<WarehouseDiscrepancy> discrepancies, @NonNull ExcelData excelData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(EXCEL_SHEET_NAME);
            
            CellStyle headerStyle = styleHelper.createHeaderStyle(workbook);
            CellStyle dataStyle = styleHelper.createDataStyle(workbook);
            CellStyle lossStyle = createLossStyle(workbook, dataStyle);
            CellStyle gainStyle = createGainStyle(workbook, dataStyle);
            
            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, discrepancies, excelData, dataStyle, lossStyle, gainStyle);
            autoSizeColumns(sheet);
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
    
    private CellStyle createLossStyle(@NonNull Workbook workbook, @NonNull CellStyle baseStyle) {
        CellStyle lossStyle = workbook.createCellStyle();
        lossStyle.cloneStyleFrom(baseStyle);
        Font lossFont = workbook.createFont();
        lossFont.setColor(IndexedColors.RED.getIndex());
        lossStyle.setFont(lossFont);
        return lossStyle;
    }
    
    private CellStyle createGainStyle(@NonNull Workbook workbook, @NonNull CellStyle baseStyle) {
        CellStyle gainStyle = workbook.createCellStyle();
        gainStyle.cloneStyleFrom(baseStyle);
        Font gainFont = workbook.createFont();
        gainFont.setColor(IndexedColors.GREEN.getIndex());
        gainStyle.setFont(gainFont);
        return gainStyle;
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
            @NonNull List<WarehouseDiscrepancy> discrepancies,
            @NonNull ExcelData excelData,
            @NonNull CellStyle dataStyle,
            @NonNull CellStyle lossStyle,
            @NonNull CellStyle gainStyle) {
        
        if (discrepancies.isEmpty()) {
            return;
        }
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);
        int rowIndex = 1;
        
        for (WarehouseDiscrepancy discrepancy : discrepancies) {
            Row row = sheet.createRow(rowIndex);
            fillDiscrepancyRow(row, discrepancy, rowIndex, excelData, dateFormatter, dataStyle, lossStyle, gainStyle);
            rowIndex++;
        }
    }
    
    private void fillDiscrepancyRow(
            @NonNull Row row,
            @NonNull WarehouseDiscrepancy discrepancy,
            int rowNumber,
            @NonNull ExcelData excelData,
            @NonNull DateTimeFormatter dateFormatter,
            @NonNull CellStyle dataStyle,
            @NonNull CellStyle lossStyle,
            @NonNull CellStyle gainStyle) {
        
        boolean isLoss = discrepancy.getType() == WarehouseDiscrepancy.DiscrepancyType.LOSS;
        CellStyle typeStyle = isLoss ? lossStyle : gainStyle;
        
        String productName = dataFetcher.getProductName(discrepancy.getProductId(), excelData.productNames());
        String warehouseName = dataFetcher.getWarehouseName(discrepancy.getWarehouseId(), excelData.warehouseNames());
        String typeText = isLoss ? TYPE_LOSS : TYPE_GAIN;
        
        createCell(row, 0, rowNumber, dataStyle);
        createCell(row, 1, formatDate(discrepancy.getReceiptDate(), dateFormatter), dataStyle);
        createCell(row, 2, discrepancy.getDriverId(), dataStyle);
        createCell(row, 3, productName, dataStyle);
        createCell(row, 4, warehouseName, dataStyle);
        createCell(row, 5, getBigDecimalValue(discrepancy.getPurchasedQuantity()), dataStyle);
        createCell(row, 6, getBigDecimalValue(discrepancy.getReceivedQuantity()), dataStyle);
        createCell(row, 7, getBigDecimalValue(discrepancy.getDiscrepancyQuantity()), typeStyle);
        createCell(row, 8, getBigDecimalValue(discrepancy.getUnitPriceEur()), dataStyle);
        createCell(row, 9, getBigDecimalValue(discrepancy.getDiscrepancyValueEur()), typeStyle);
        createCell(row, 10, typeText, typeStyle);
        createCell(row, 11, discrepancy.getComment() != null ? discrepancy.getComment() : "", dataStyle);
    }
    
    private String formatDate(LocalDate date, @NonNull DateTimeFormatter formatter) {
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
