package org.example.userservice.services.transaction;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class TransactionExcelBuilder {

    private static final String SHEET_NAME = "Транзакції";
    private static final String NO_TRANSACTIONS_MESSAGE = "Транзакції не знайдено";
    private static final String DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm";
    private static final short HEADER_FONT_SIZE = 12;
    private static final int COLUMN_COUNT = 15;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

    private static final String[] HEADERS = {
            "Дата", "Тип", "Категорія", "З рахунку", "На рахунок",
            "Сума списання", "Валюта", "Комісія", "Сума переказу/зачислення",
            "Курс конвертації", "У валюту", "Конвертована сума",
            "Клієнт", "Контрагент", "Опис"
    };

    private static final Map<String, String> TYPE_MAP = Map.of(
            "INTERNAL_TRANSFER", "Перевод між рахунками",
            "EXTERNAL_INCOME", "Зовнішній прихід",
            "EXTERNAL_EXPENSE", "Зовнішній витрата",
            "CLIENT_PAYMENT", "Оплата клієнту",
            "CURRENCY_CONVERSION", "Конвертація валют",
            "VEHICLE_EXPENSE", "Витрати на машину"
    );

    public byte[] buildEmpty() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue(NO_TRANSACTIONS_MESSAGE);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public byte[] buildWithData(List<Transaction> transactions,
                                Map<Long, String> clientCompanyMap,
                                Map<Long, String> accountNameMap,
                                Map<Long, String> categoryNameMap,
                                Map<Long, String> counterpartyNameMap) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            createHeaderRow(sheet, headerStyle);
            fillDataRows(sheet, transactions, clientCompanyMap, accountNameMap,
                    categoryNameMap, counterpartyNameMap, dataStyle);
            autoSizeColumns(sheet);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
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

    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillDataRows(Sheet sheet,
                              List<Transaction> transactions,
                              Map<Long, String> clientCompanyMap,
                              Map<Long, String> accountNameMap,
                              Map<Long, String> categoryNameMap,
                              Map<Long, String> counterpartyNameMap,
                              CellStyle dataStyle) {
        int rowNum = 1;
        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            fillTransactionRow(row, transaction, clientCompanyMap, accountNameMap,
                    categoryNameMap, counterpartyNameMap, dataStyle);
        }
    }

    private void fillTransactionRow(Row row,
                                     Transaction transaction,
                                     Map<Long, String> clientCompanyMap,
                                     Map<Long, String> accountNameMap,
                                     Map<Long, String> categoryNameMap,
                                     Map<Long, String> counterpartyNameMap,
                                     CellStyle dataStyle) {
        createCell(row, 0, formatDate(transaction.getCreatedAt()), dataStyle);
        createCell(row, 1, getTransactionTypeName(transaction.getType()), dataStyle);
        createCell(row, 2, getCategoryName(transaction.getCategoryId(), categoryNameMap), dataStyle);
        createCell(row, 3, getAccountName(transaction.getFromAccountId(), accountNameMap), dataStyle);
        createCell(row, 4, getAccountName(transaction.getToAccountId(), accountNameMap), dataStyle);
        createCell(row, 5, getBigDecimalValue(transaction.getAmount()), dataStyle);
        createCell(row, 6, transaction.getCurrency(), dataStyle);
        createCell(row, 7, getCommissionValue(transaction.getCommission()), dataStyle);
        createCell(row, 8, getTransferAmount(transaction), dataStyle);
        createCell(row, 9, getBigDecimalValue(transaction.getExchangeRate()), dataStyle);
        createCell(row, 10, transaction.getConvertedCurrency(), dataStyle);
        createCell(row, 11, getBigDecimalValue(transaction.getConvertedAmount()), dataStyle);
        createCell(row, 12, getClientCompany(transaction.getClientId(), clientCompanyMap), dataStyle);
        createCell(row, 13, getCounterpartyName(transaction.getCounterpartyId(), counterpartyNameMap), dataStyle);
        createCell(row, 14, transaction.getDescription(), dataStyle);
    }

    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);

        switch (value) {
            case null -> cell.setCellValue("");
            case BigDecimal bigDecimal -> cell.setCellValue(bigDecimal.doubleValue());
            case Number number -> {
                if (value instanceof Long || value instanceof Integer) {
                    cell.setCellValue(number.longValue());
                } else {
                    cell.setCellValue(number.doubleValue());
                }
            }
            case String string -> cell.setCellValue(string);
            default -> cell.setCellValue(value.toString());
        }
    }

    private String formatDate(java.time.LocalDateTime date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String getTransactionTypeName(TransactionType type) {
        if (type == null) {
            return "";
        }
        return TYPE_MAP.getOrDefault(type.name(), type.name());
    }

    private String getCategoryName(Long categoryId, Map<Long, String> categoryNameMap) {
        return categoryId != null ? categoryNameMap.getOrDefault(categoryId, "") : "";
    }

    private String getAccountName(Long accountId, Map<Long, String> accountNameMap) {
        return accountId != null ? accountNameMap.getOrDefault(accountId, "") : "";
    }

    private BigDecimal getBigDecimalValue(BigDecimal value) {
        return value;
    }

    private BigDecimal getCommissionValue(BigDecimal commission) {
        return commission != null && commission.compareTo(BigDecimal.ZERO) > 0 ? commission : null;
    }

    private BigDecimal getTransferAmount(Transaction transaction) {
        if (transaction.getType() == TransactionType.INTERNAL_TRANSFER) {
            if (transaction.getAmount() != null) {
                BigDecimal transferAmount = transaction.getAmount();
                if (transaction.getCommission() != null) {
                    transferAmount = transferAmount.subtract(transaction.getCommission());
                }
                return transferAmount;
            }
        } else if (transaction.getType() == TransactionType.CURRENCY_CONVERSION) {
            return transaction.getConvertedAmount();
        }
        return null;
    }

    private String getClientCompany(Long clientId, Map<Long, String> clientCompanyMap) {
        return clientId != null ? clientCompanyMap.getOrDefault(clientId, "") : "";
    }

    private String getCounterpartyName(Long counterpartyId, Map<Long, String> counterpartyNameMap) {
        return counterpartyId != null ? counterpartyNameMap.getOrDefault(counterpartyId, "") : "";
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
