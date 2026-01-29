package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.mappers.VehicleMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VehicleExcelGenerator {
    
    private final VehicleMapper vehicleMapper;
    private final VehicleFinancialCalculator financialCalculator;
    private final VehicleFieldValueFormatter formatter;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String SHEET_NAME = "Машини";
    private static final short HEADER_FONT_SIZE = 12;
    private static final String DATE_FORMAT_PATTERN = "dd.mm.yyyy";
    private static final String NUMBER_FORMAT_PATTERN = "#,##0.00";
    private static final int DEFAULT_COLUMN_WIDTH = 4000;
    
    public record ExcelStyles(CellStyle headerStyle, CellStyle dataStyle, CellStyle dateStyle, CellStyle numberStyle) {}
    
    public byte[] generateWorkbook(@NonNull List<Vehicle> vehicles,
                                   @NonNull VehicleExportDataFetcher.VehicleData vehicleData,
                                   @NonNull List<String> headerList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            ExcelStyles styles = createExcelStyles(workbook);
            
            createHeaderRow(sheet, headerList, styles.headerStyle());
            createDataRows(sheet, vehicles, vehicleData, styles);
            setFixedColumnWidths(sheet, headerList.size());
            
            return writeWorkbookToBytes(workbook);
        }
    }
    
    public byte[] createEmptyWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue("Машини не знайдено");
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private ExcelStyles createExcelStyles(Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook, dataStyle);
        CellStyle numberStyle = createNumberStyle(workbook, dataStyle);
        return new ExcelStyles(headerStyle, dataStyle, dateStyle, numberStyle);
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints(HEADER_FONT_SIZE);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        return dataStyle;
    }
    
    private CellStyle createDateStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(baseStyle);
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat(DATE_FORMAT_PATTERN));
        return dateStyle;
    }
    
    private CellStyle createNumberStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(baseStyle);
        CreationHelper createHelper = workbook.getCreationHelper();
        numberStyle.setDataFormat(createHelper.createDataFormat().getFormat(NUMBER_FORMAT_PATTERN));
        return numberStyle;
    }
    
    private void createHeaderRow(Sheet sheet, List<String> headerList, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headerList.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerList.get(i));
            cell.setCellStyle(headerStyle);
        }
    }
    
    private void createDataRows(Sheet sheet, List<Vehicle> vehicles, 
                               VehicleExportDataFetcher.VehicleData vehicleData, ExcelStyles styles) {
        int rowIndex = 1;
        for (Vehicle vehicle : vehicles) {
            VehicleDetailsDTO vehicleDTO = vehicleMapper.vehicleToVehicleDetailsDTO(vehicle);
            List<org.example.purchaseservice.models.balance.VehicleExpense> expenses = 
                    vehicleData.expensesMap().getOrDefault(vehicle.getId(), Collections.emptyList());
            
            Row vehicleRow = sheet.createRow(rowIndex++);
            writeVehicleRow(vehicleRow, vehicleDTO, expenses, vehicleData, styles);
        }
    }
    
    private void writeVehicleRow(Row row, VehicleDetailsDTO vehicleDTO, 
                                 List<org.example.purchaseservice.models.balance.VehicleExpense> expenses,
                                 VehicleExportDataFetcher.VehicleData vehicleData, ExcelStyles styles) {
        int columnIndex = 0;
        
        VehicleFinancialCalculator.VehicleProductsData productsData = 
                financialCalculator.calculateVehicleProductsData(vehicleDTO, vehicleData.productMap(), formatter);
        VehicleFinancialCalculator.VehicleExpensesData expensesData = 
                financialCalculator.calculateVehicleExpensesData(expenses);
        VehicleFinancialCalculator.FinancialMetrics financialMetrics = 
                financialCalculator.calculateFinancialMetrics(productsData, expensesData, vehicleDTO);
        
        columnIndex = writeFinancialColumns(row, columnIndex, productsData, expensesData, styles);
        columnIndex = writeInvoiceColumns(row, columnIndex, vehicleDTO, styles);
        columnIndex = writeReclamationColumns(row, columnIndex, financialMetrics.reclamationData(), styles);
        columnIndex = writeSummaryColumns(row, columnIndex, financialMetrics, styles);
        columnIndex = writeProductsColumn(row, columnIndex, productsData, styles);
        columnIndex = writeExpenseCategoryColumns(row, columnIndex, expenses, vehicleData, styles);
        columnIndex = writeVehicleInfoColumns(row, columnIndex, vehicleDTO, styles);
        writeCarrierColumns(row, columnIndex, vehicleDTO, styles);
    }
    
    private int writeFinancialColumns(Row row, int columnIndex, 
                                     VehicleFinancialCalculator.VehicleProductsData productsData,
                                     VehicleFinancialCalculator.VehicleExpensesData expensesData,
                                     ExcelStyles styles) {
        setCellValue(row, columnIndex++, productsData.totalCost(), styles.numberStyle());
        setCellValue(row, columnIndex++, expensesData.total(), styles.numberStyle());
        return columnIndex;
    }
    
    private int writeInvoiceColumns(Row row, int columnIndex, VehicleDetailsDTO vehicleDTO, ExcelStyles styles) {
        setCellValue(row, columnIndex++, vehicleDTO.getProduct(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getProductQuantity(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceUa(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceUaDate(), styles.dateStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceUaPricePerTon(), styles.numberStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceUaTotalPrice(), styles.numberStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceEu(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceEuDate(), styles.dateStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceEuPricePerTon(), styles.numberStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getInvoiceEuTotalPrice(), styles.numberStyle());
        return columnIndex;
    }
    
    private int writeReclamationColumns(Row row, int columnIndex, 
                                       VehicleFinancialCalculator.ReclamationData reclamationData, 
                                       ExcelStyles styles) {
        setCellValue(row, columnIndex++, reclamationData.reclamationPerTon(), styles.numberStyle());
        setCellValue(row, columnIndex++, reclamationData.fullReclamation(), styles.numberStyle());
        return columnIndex;
    }
    
    private int writeSummaryColumns(Row row, int columnIndex, 
                                   VehicleFinancialCalculator.FinancialMetrics financialMetrics, 
                                   ExcelStyles styles) {
        setCellValue(row, columnIndex++, financialMetrics.totalExpenses(), styles.numberStyle());
        setCellValue(row, columnIndex++, financialMetrics.totalIncome(), styles.numberStyle());
        setCellValue(row, columnIndex++, financialMetrics.margin(), styles.numberStyle());
        return columnIndex;
    }
    
    private int writeProductsColumn(Row row, int columnIndex, 
                                   VehicleFinancialCalculator.VehicleProductsData productsData, 
                                   ExcelStyles styles) {
        setCellValue(row, columnIndex++, productsData.productsText(), styles.dataStyle());
        return columnIndex;
    }
    
    private int writeExpenseCategoryColumns(Row row, int columnIndex,
                                           List<org.example.purchaseservice.models.balance.VehicleExpense> expenses,
                                           VehicleExportDataFetcher.VehicleData vehicleData, ExcelStyles styles) {
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesByCategoryMap = 
                groupExpensesByCategory(expenses);
        
        for (Long categoryId : vehicleData.sortedCategoryIds()) {
            List<org.example.purchaseservice.models.balance.VehicleExpense> categoryExpenses = 
                    expensesByCategoryMap.getOrDefault(categoryId, Collections.emptyList());
            String expenseDetails = formatter.formatExpenseDetails(categoryExpenses, vehicleData.accountNameMap());
            setCellValue(row, columnIndex++, expenseDetails, styles.dataStyle());
        }
        
        return columnIndex;
    }
    
    private Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> groupExpensesByCategory(
            List<org.example.purchaseservice.models.balance.VehicleExpense> expenses) {
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesByCategoryMap = new HashMap<>();
        if (isNotEmpty(expenses)) {
            for (org.example.purchaseservice.models.balance.VehicleExpense expense : expenses) {
                Long categoryId = expense.getCategoryId();
                if (categoryId != null) {
                    expensesByCategoryMap.computeIfAbsent(categoryId, _ -> new ArrayList<>()).add(expense);
                }
            }
        }
        return expensesByCategoryMap;
    }
    
    private int writeVehicleInfoColumns(Row row, int columnIndex, VehicleDetailsDTO vehicleDTO, ExcelStyles styles) {
        setCellValue(row, columnIndex++, vehicleDTO.getId(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getShipmentDate(), styles.dateStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getVehicleNumber(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDescription(), styles.dataStyle());
        setCellValue(row, columnIndex++, formatter.formatBoolean(vehicleDTO.getIsOurVehicle()), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getSenderName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getReceiverName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getTerminalName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDestinationCountryName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDestinationPlaceName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDeclarationNumber(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDriverFullName(), styles.dataStyle());
        setCellValue(row, columnIndex++, formatter.formatBoolean(vehicleDTO.getEur1()), styles.dataStyle());
        setCellValue(row, columnIndex++, formatter.formatBoolean(vehicleDTO.getFito()), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getCustomsDate(), styles.dateStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getCustomsClearanceDate(), styles.dateStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getUnloadingDate(), styles.dateStyle());
        return columnIndex;
    }
    
    private void writeCarrierColumns(Row row, int columnIndex, VehicleDetailsDTO vehicleDTO, ExcelStyles styles) {
        if (vehicleDTO.getCarrier() != null) {
            setCellValue(row, columnIndex++, vehicleDTO.getCarrier().getCompanyName(), styles.dataStyle());
            setCellValue(row, columnIndex++, vehicleDTO.getCarrier().getRegistrationAddress(), styles.dataStyle());
            setCellValue(row, columnIndex++, vehicleDTO.getCarrier().getPhoneNumber(), styles.dataStyle());
            setCellValue(row, columnIndex++, vehicleDTO.getCarrier().getCode(), styles.dataStyle());
            setCellValue(row, columnIndex, vehicleDTO.getCarrier().getAccount(), styles.dataStyle());
        } else {
            writeEmptyCells(row, columnIndex, styles.dataStyle());
        }
    }
    
    private void writeEmptyCells(Row row, int startColumn, CellStyle style) {
        for (int i = 0; i < 5; i++) {
            setCellValue(row, startColumn + i, "", style);
        }
    }
    
    private void setFixedColumnWidths(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.setColumnWidth(i, DEFAULT_COLUMN_WIDTH);
        }
    }
    
    private byte[] writeWorkbookToBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private void setCellValue(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        switch (value) {
            case null -> cell.setCellValue("");
            case String s -> cell.setCellValue(s);
            case Number number -> cell.setCellValue(number.doubleValue());
            case LocalDate localDate -> cell.setCellValue(localDate.format(DATE_FORMATTER));
            default -> cell.setCellValue(value.toString());
        }
    }
    
    private boolean isNotEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
