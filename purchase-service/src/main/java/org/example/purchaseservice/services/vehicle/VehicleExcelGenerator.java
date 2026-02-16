package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.mappers.VehicleMapper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VehicleExcelGenerator {
    
    private final VehicleMapper vehicleMapper;
    private final VehicleFinancialCalculator financialCalculator;
    private final VehicleFieldValueFormatter formatter;
    private final ExcelStylesService excelStylesService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String SHEET_NAME = "Машини";
    private static final int DEFAULT_COLUMN_WIDTH = 4000;
    
    public record ExcelStyles(CellStyle headerStyle, CellStyle dataStyle, CellStyle dateStyle, CellStyle numberStyle) {}
    
    public byte[] generateWorkbook(@NonNull List<Vehicle> vehicles,
                                   @NonNull VehicleExportDataFetcher.VehicleData vehicleData,
                                   @NonNull List<String> headerList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            ExcelStyles styles = excelStylesService.createExcelStyles(workbook);
            
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
                                 List<VehicleExpense> expenses,
                                 VehicleExportDataFetcher.VehicleData vehicleData,
                                 ExcelStyles styles) {

        VehicleFinancialCalculator.VehicleProductsData productsData =
                financialCalculator.calculateVehicleProductsData(vehicleDTO, vehicleData.productMap(), formatter);

        VehicleFinancialCalculator.VehicleExpensesData expensesData =
                financialCalculator.calculateVehicleExpensesData(expenses);

        VehicleFinancialCalculator.FinancialMetrics financialMetrics =
                financialCalculator.calculateFinancialMetrics(productsData, expensesData, vehicleDTO);

        VehicleFinancialCalculator.ReclamationData reclamation = financialMetrics.reclamationData();

        Map<Long, List<VehicleExpense>> expensesByCategory = groupExpensesByCategory(expenses);

        int col = 0;

        col = writeSenderAndReceiver(row, col, vehicleDTO, styles);
        col = writeVehicleAndEuInvoice(row, col, vehicleDTO, styles);
        col = writeReclamationAndIncome(row, col, reclamation, financialMetrics, styles);
        col = writeWarehouseCost(row, col, productsData, styles);
        col = writeExpenseCategories(row, col, vehicleData, expensesByCategory, styles);
        col = writeTotalExpensesAndMargin(row, col, financialMetrics, styles);
        col = writeUaInvoice(row, col, vehicleDTO, styles);
        col = writeAdditionalVehicleInfo(row, col, vehicleDTO, vehicleData, styles);
        writeCarrierInfo(row, col, vehicleDTO, styles);
    }

    private int writeSenderAndReceiver(Row row, int col, VehicleDetailsDTO dto, ExcelStyles styles) {
        setCellValue(row, col++, dto.getSenderName(), styles.dataStyle());
        setCellValue(row, col++, dto.getReceiverName(), styles.dataStyle());
        return col;
    }

    private int writeVehicleAndEuInvoice(Row row, int col, VehicleDetailsDTO dto, ExcelStyles styles) {
        setCellValue(row, col++, dto.getVehicleNumber(), styles.dataStyle());
        setCellValue(row, col++, dto.getInvoiceEuDate(), styles.dateStyle());
        setCellValue(row, col++, dto.getInvoiceEu(), styles.dataStyle());
        setCellValue(row, col++, dto.getProductQuantity(), styles.dataStyle());
        setCellValue(row, col++, dto.getInvoiceEuPricePerTon(), styles.numberStyle());
        setCellValue(row, col++, dto.getInvoiceEuTotalPrice(), styles.numberStyle());
        return col;
    }

    private int writeReclamationAndIncome(Row row, int col,
                                          VehicleFinancialCalculator.ReclamationData reclamation,
                                          VehicleFinancialCalculator.FinancialMetrics metrics,
                                          ExcelStyles styles) {
        setCellValue(row, col++, reclamation.reclamationPerTon(), styles.numberStyle());
        setCellValue(row, col++, reclamation.fullReclamation(), styles.numberStyle());
        setCellValue(row, col++, metrics.totalIncome(), styles.numberStyle());
        return col;
    }

    private int writeWarehouseCost(Row row, int col,
                                   VehicleFinancialCalculator.VehicleProductsData productsData,
                                   ExcelStyles styles) {
        setCellValue(row, col++, productsData.totalCost(), styles.numberStyle());
        return col;
    }

    private int writeExpenseCategories(Row row, int col,
                                       VehicleExportDataFetcher.VehicleData vehicleData,
                                       Map<Long, List<VehicleExpense>> expensesByCategory,
                                       ExcelStyles styles) {
        for (Long categoryId : vehicleData.sortedCategoryIds()) {
            List<VehicleExpense> catExpenses = expensesByCategory.getOrDefault(categoryId, Collections.emptyList());

            BigDecimal totalEur = catExpenses.stream()
                    .map(VehicleExpense::getConvertedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String details = formatter.formatExpenseDetails(catExpenses, vehicleData.accountNameMap());

            setCellValue(row, col++, totalEur, styles.numberStyle());
            setCellValue(row, col++, details, styles.dataStyle());
        }
        return col;
    }

    private int writeTotalExpensesAndMargin(Row row, int col,
                                            VehicleFinancialCalculator.FinancialMetrics metrics,
                                            ExcelStyles styles) {
        setCellValue(row, col++, metrics.totalExpenses(), styles.numberStyle());
        setCellValue(row, col++, metrics.margin(), styles.numberStyle());
        return col;
    }

    private int writeUaInvoice(Row row, int col, VehicleDetailsDTO dto, ExcelStyles styles) {
        setCellValue(row, col++, dto.getInvoiceUa(), styles.dataStyle());
        setCellValue(row, col++, dto.getInvoiceUaDate(), styles.dateStyle());
        setCellValue(row, col++, dto.getInvoiceUaPricePerTon(), styles.numberStyle());
        setCellValue(row, col++, dto.getInvoiceUaTotalPrice(), styles.numberStyle());
        return col;
    }

    private int writeAdditionalVehicleInfo(Row row, int col, VehicleDetailsDTO dto,
                                           VehicleExportDataFetcher.VehicleData vehicleData,
                                           ExcelStyles styles) {
        setCellValue(row, col++, dto.getId(), styles.dataStyle());
        setCellValue(row, col++, dto.getShipmentDate(), styles.dateStyle());
        setCellValue(row, col++, dto.getDescription(), styles.dataStyle());
        String managerName = vehicleData.managerNameMap() != null && dto.getManagerId() != null
                ? vehicleData.managerNameMap().getOrDefault(dto.getManagerId(), "")
                : "";
        setCellValue(row, col++, managerName, styles.dataStyle());
        setCellValue(row, col++, dto.getProduct(), styles.dataStyle());
        setCellValue(row, col++, dto.getDestinationCountryName(), styles.dataStyle());
        setCellValue(row, col++, dto.getDestinationPlaceName(), styles.dataStyle());
        setCellValue(row, col++, dto.getDeclarationNumber(), styles.dataStyle());
        setCellValue(row, col++, dto.getTerminalName(), styles.dataStyle());
        setCellValue(row, col++, dto.getDriverFullName(), styles.dataStyle());
        setCellValue(row, col++, formatter.formatBoolean(dto.getEur1()), styles.dataStyle());
        setCellValue(row, col++, formatter.formatBoolean(dto.getFito()), styles.dataStyle());
        setCellValue(row, col++, dto.getCustomsDate(), styles.dateStyle());
        setCellValue(row, col++, dto.getCustomsClearanceDate(), styles.dateStyle());
        setCellValue(row, col++, dto.getUnloadingDate(), styles.dateStyle());
        return col;
    }

    private void writeCarrierInfo(Row row, int col, VehicleDetailsDTO dto, ExcelStyles styles) {
        if (dto.getCarrier() != null) {
            setCellValue(row, col++, dto.getCarrier().getCompanyName(), styles.dataStyle());
            setCellValue(row, col++, dto.getCarrier().getRegistrationAddress(), styles.dataStyle());
            setCellValue(row, col++, dto.getCarrier().getPhoneNumber(), styles.dataStyle());
            setCellValue(row, col++, dto.getCarrier().getCode(), styles.dataStyle());
            setCellValue(row, col,   dto.getCarrier().getAccount(), styles.dataStyle());
        } else {
            writeEmptyCells(row, col, styles.dataStyle());
        }
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
