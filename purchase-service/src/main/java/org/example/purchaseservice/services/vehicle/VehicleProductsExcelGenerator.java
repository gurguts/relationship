package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleProductsExcelGenerator {

    private final ExcelStylesService excelStylesService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String SHEET_NAME = "Товари машин";
    private static final int DEFAULT_COLUMN_WIDTH = 4000;

    private static final String[] HEADERS = {
            "ID машини",
            "Номер машини",
            "Склад",
            "Товар",
            "Кількість",
            "Ціна за кг",
            "Загальна вартість",
            "Дата списання",
            "Менеджер",
            "Коментар"
    };

    public byte[] generate(@NonNull List<VehicleProductExcelRow> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            VehicleExcelGenerator.ExcelStyles styles = excelStylesService.createExcelStyles(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(styles.headerStyle());
            }

            int rowIndex = 1;
            for (VehicleProductExcelRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                setCellValue(dataRow, 0, row.vehicleId(), styles.dataStyle());
                setCellValue(dataRow, 1, row.vehicleNumber(), styles.dataStyle());
                setCellValue(dataRow, 2, row.warehouseName(), styles.dataStyle());
                setCellValue(dataRow, 3, row.productName(), styles.dataStyle());
                setCellValue(dataRow, 4, row.quantity(), styles.numberStyle());
                setCellValue(dataRow, 5, row.pricePerKg(), styles.numberStyle());
                setCellValue(dataRow, 6, row.totalCost(), styles.numberStyle());
                setCellValue(dataRow, 7, row.withdrawalDate(), styles.dateStyle());
                setCellValue(dataRow, 8, row.managerName(), styles.dataStyle());
                setCellValue(dataRow, 9, row.vehicleComment(), styles.dataStyle());
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.setColumnWidth(i, DEFAULT_COLUMN_WIDTH);
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    public byte[] createEmptyWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            VehicleExcelGenerator.ExcelStyles styles = excelStylesService.createExcelStyles(workbook);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(styles.headerStyle());
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void setCellValue(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String s) {
            cell.setCellValue(s);
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (value instanceof LocalDate localDate) {
            cell.setCellValue(localDate.format(DATE_FORMATTER));
        } else if (value instanceof Long l) {
            cell.setCellValue(l.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    public record VehicleProductExcelRow(
            Long vehicleId,
            String vehicleNumber,
            String warehouseName,
            String productName,
            BigDecimal quantity,
            BigDecimal pricePerKg,
            BigDecimal totalCost,
            LocalDate withdrawalDate,
            String managerName,
            String vehicleComment
    ) {}
}
