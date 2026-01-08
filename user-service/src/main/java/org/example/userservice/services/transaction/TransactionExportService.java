package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.userservice.clients.ClientApiClient;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.models.transaction.TransactionType;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.repositories.CounterpartyRepository;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.spec.TransactionSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionExportService {
    private static final String ERROR_CODE_EXPORT_FAILED = "EXPORT_FAILED";
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

    private final TransactionRepository transactionRepository;
    private final ClientApiClient clientApiClient;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final CounterpartyRepository counterpartyRepository;

    @Transactional(readOnly = true)
    public byte[] exportToExcel(@NonNull Map<String, List<String>> filters) {
        log.info("Starting transaction export with filters: {}", filters);
        
        try {
            TransactionSpecification spec = new TransactionSpecification(filters);
            Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
            List<Transaction> transactions = transactionRepository.findAll(spec, sortBy);
            
            log.info("Found {} transactions for export", transactions.size());

            if (transactions.isEmpty()) {
                return createEmptyWorkbook();
            }

            Map<Long, String> clientCompanyMap = loadClientData(transactions);
            Map<Long, String> accountNameMap = loadAccountData(transactions);
            Map<Long, String> categoryNameMap = loadCategoryData(transactions);
            Map<Long, String> counterpartyNameMap = loadCounterpartyData(transactions);

            return createWorkbookWithData(transactions, clientCompanyMap, accountNameMap, 
                    categoryNameMap, counterpartyNameMap);
        } catch (Exception e) {
            log.error("Failed to export transactions: {}", e.getMessage(), e);
            throw new TransactionException(ERROR_CODE_EXPORT_FAILED, "Failed to export transactions: " + e.getMessage());
        }
    }

    private byte[] createEmptyWorkbook() throws IOException {
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

    private Map<Long, String> loadClientData(@NonNull List<Transaction> transactions) {
        List<Long> clientIds = transactions.stream()
                .map(Transaction::getClientId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return Optional.ofNullable(clientApiClient.getClients(clientIds).getBody())
                    .map(clients -> clients.stream()
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, _) -> a
                            )))
                    .orElse(Collections.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to load client data: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, String> loadAccountData(@NonNull List<Transaction> transactions) {
        Set<Long> accountIds = transactions.stream()
                .flatMap(t -> {
                    if (t.getFromAccountId() != null && t.getToAccountId() != null) {
                        return java.util.stream.Stream.of(t.getFromAccountId(), t.getToAccountId());
                    } else if (t.getFromAccountId() != null) {
                        return java.util.stream.Stream.of(t.getFromAccountId());
                    } else if (t.getToAccountId() != null) {
                        return java.util.stream.Stream.of(t.getToAccountId());
                    }
                    return java.util.stream.Stream.empty();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(accountRepository.findAllById(accountIds).spliterator(), false)
                .collect(Collectors.toMap(Account::getId, Account::getName));
    }

    private Map<Long, String> loadCategoryData(@NonNull List<Transaction> transactions) {
        Set<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(transactionCategoryRepository.findAllById(categoryIds).spliterator(), false)
                .collect(Collectors.toMap(TransactionCategory::getId, TransactionCategory::getName));
    }

    private Map<Long, String> loadCounterpartyData(@NonNull List<Transaction> transactions) {
        Set<Long> counterpartyIds = transactions.stream()
                .map(Transaction::getCounterpartyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (counterpartyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(counterpartyRepository.findAllById(counterpartyIds).spliterator(), false)
                .collect(Collectors.toMap(Counterparty::getId, Counterparty::getName));
    }

    private byte[] createWorkbookWithData(@NonNull List<Transaction> transactions,
                                          @NonNull Map<Long, String> clientCompanyMap,
                                          @NonNull Map<Long, String> accountNameMap,
                                          @NonNull Map<Long, String> categoryNameMap,
                                          @NonNull Map<Long, String> counterpartyNameMap) throws IOException {
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
            log.info("Successfully exported {} transactions to Excel", transactions.size());
            return outputStream.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(@NonNull Workbook workbook) {
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

    private CellStyle createDataStyle(@NonNull Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        return dataStyle;
    }

    private void createHeaderRow(@NonNull Sheet sheet, @NonNull CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void fillDataRows(@NonNull Sheet sheet,
                              @NonNull List<Transaction> transactions,
                              @NonNull Map<Long, String> clientCompanyMap,
                              @NonNull Map<Long, String> accountNameMap,
                              @NonNull Map<Long, String> categoryNameMap,
                              @NonNull Map<Long, String> counterpartyNameMap,
                              @NonNull CellStyle dataStyle) {
        int rowNum = 1;
        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            fillTransactionRow(row, transaction, clientCompanyMap, accountNameMap, 
                    categoryNameMap, counterpartyNameMap, dataStyle);
        }
    }

    private void fillTransactionRow(@NonNull Row row,
                                    @NonNull Transaction transaction,
                                    @NonNull Map<Long, String> clientCompanyMap,
                                    @NonNull Map<Long, String> accountNameMap,
                                    @NonNull Map<Long, String> categoryNameMap,
                                    @NonNull Map<Long, String> counterpartyNameMap,
                                    @NonNull CellStyle dataStyle) {
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

    private void createCell(@NonNull Row row, int columnIndex, Object value, @NonNull CellStyle style) {
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

    private String getCategoryName(Long categoryId, @NonNull Map<Long, String> categoryNameMap) {
        return categoryId != null ? categoryNameMap.getOrDefault(categoryId, "") : "";
    }

    private String getAccountName(Long accountId, @NonNull Map<Long, String> accountNameMap) {
        return accountId != null ? accountNameMap.getOrDefault(accountId, "") : "";
    }

    private BigDecimal getBigDecimalValue(BigDecimal value) {
        return value;
    }

    private BigDecimal getCommissionValue(BigDecimal commission) {
        return commission != null && commission.compareTo(BigDecimal.ZERO) > 0 ? commission : null;
    }

    private BigDecimal getTransferAmount(@NonNull Transaction transaction) {
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

    private String getClientCompany(Long clientId, @NonNull Map<Long, String> clientCompanyMap) {
        return clientId != null ? clientCompanyMap.getOrDefault(clientId, "") : "";
    }

    private String getCounterpartyName(Long counterpartyId, @NonNull Map<Long, String> counterpartyNameMap) {
        return counterpartyId != null ? counterpartyNameMap.getOrDefault(counterpartyId, "") : "";
    }

    private void autoSizeColumns(@NonNull Sheet sheet) {
        for (int i = 0; i < COLUMN_COUNT; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
