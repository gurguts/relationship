package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class ExcelStyleHelper {
    
    public CellStyle createHeaderStyle(@NonNull Workbook workbook) {
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
        return headerStyle;
    }
    
    public CellStyle createDataStyle(@NonNull Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        return dataStyle;
    }
    
    public void setCellValue(@NonNull Cell cell, @NonNull Object value) {
        switch (value) {
            case BigDecimal bigDecimal -> cell.setCellValue(bigDecimal.doubleValue());
            case Number number -> {
                if (value instanceof Long || value instanceof Integer) {
                    cell.setCellValue(number.longValue());
                } else {
                    cell.setCellValue(number.doubleValue());
                }
            }
            case String s -> cell.setCellValue(s);
            default -> cell.setCellValue(value.toString());
        }
    }
    
    public void createCell(@NonNull Row row, int columnIndex, @NonNull Object value, @NonNull CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        setCellValue(cell, value);
    }
}
