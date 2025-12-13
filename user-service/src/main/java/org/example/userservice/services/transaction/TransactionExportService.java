package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.userservice.clients.ClientApiClient;
import org.example.userservice.clients.VehicleApiClient;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.spec.TransactionSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionExportService {
    private final TransactionRepository transactionRepository;
    private final ClientApiClient clientApiClient;
    private final VehicleApiClient vehicleApiClient;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] exportToExcel(Map<String, List<String>> filters) throws IOException {
        // Get all transactions with filters (no pagination)
        TransactionSpecification spec = new TransactionSpecification(filters);
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Transaction> transactions = transactionRepository.findAll(spec, sortBy);

        if (transactions.isEmpty()) {
            // Return empty workbook
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Транзакції");
                Row headerRow = sheet.createRow(0);
                Cell headerCell = headerRow.createCell(0);
                headerCell.setCellValue("Транзакції не знайдено");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }

        // Get client IDs
        List<Long> clientIds = transactions.stream()
                .map(Transaction::getClientId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> clientCompanyMap = clientIds.isEmpty()
                ? Collections.emptyMap()
                : clientApiClient.getClients(clientIds).stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, _) -> a
                ));

        // Get account IDs
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

        Map<Long, String> accountNameMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : StreamSupport.stream(accountRepository.findAllById(accountIds).spliterator(), false)
                .collect(Collectors.toMap(Account::getId, Account::getName));

        // Get category IDs
        Set<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : StreamSupport.stream(transactionCategoryRepository.findAllById(categoryIds).spliterator(), false)
                .collect(Collectors.toMap(TransactionCategory::getId, TransactionCategory::getName));

        List<Long> vehicleIds = transactions.stream()
                .map(Transaction::getVehicleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> vehicleNumberMap;
        if (vehicleIds.isEmpty()) {
            vehicleNumberMap = Collections.emptyMap();
        } else {
            try {
                List<Map<Long, String>> vehiclesResponse = vehicleApiClient.getVehicles(vehicleIds);
                vehicleNumberMap = vehiclesResponse.stream()
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, _) -> a
                        ));
            } catch (Exception e) {
                log.warn("Failed to fetch vehicle numbers: {}", e.getMessage());
                vehicleNumberMap = Collections.emptyMap();
            }
        }
        final Map<Long, String> finalVehicleNumberMap = vehicleNumberMap;

        // Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Транзакції");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Create date style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy hh:mm"));

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Дата", "Тип", "Машина", "Категорія", "З рахунку", "На рахунок",
                    "Сума списання", "Валюта", "Комісія", "Сума переказу/зачислення",
                    "Курс конвертації", "У валюту", "Конвертована сума",
                    "Клієнт", "Опис"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Map transaction types to Ukrainian
            Map<String, String> typeMap = Map.of(
                    "INTERNAL_TRANSFER", "Перевод між рахунками",
                    "EXTERNAL_INCOME", "Зовнішній прихід",
                    "EXTERNAL_EXPENSE", "Зовнішній витрата",
                    "CLIENT_PAYMENT", "Оплата клієнту",
                    "CURRENCY_CONVERSION", "Конвертація валют",
                    "VEHICLE_EXPENSE", "Витрати на машину"
            );

            // Create data rows
            int rowNum = 1;
            for (Transaction transaction : transactions) {
                Row row = sheet.createRow(rowNum++);

                // Date
                Cell dateCell = row.createCell(0);
                if (transaction.getCreatedAt() != null) {
                    dateCell.setCellValue(transaction.getCreatedAt().format(DATE_FORMATTER));
                } else {
                    dateCell.setCellValue("");
                }
                dateCell.setCellStyle(dataStyle);

                // Type
                Cell typeCell = row.createCell(1);
                String type = transaction.getType() != null ? transaction.getType().name() : "";
                typeCell.setCellValue(typeMap.getOrDefault(type, type));
                typeCell.setCellStyle(dataStyle);

                // Vehicle
                Cell vehicleCell = row.createCell(2);
                String vehicleNumber = transaction.getVehicleId() != null
                        ? finalVehicleNumberMap.getOrDefault(transaction.getVehicleId(), "")
                        : "";
                vehicleCell.setCellValue(vehicleNumber);
                vehicleCell.setCellStyle(dataStyle);

                // Category
                Cell categoryCell = row.createCell(3);
                String categoryName = transaction.getCategoryId() != null
                        ? categoryNameMap.getOrDefault(transaction.getCategoryId(), "")
                        : "";
                categoryCell.setCellValue(categoryName);
                categoryCell.setCellStyle(dataStyle);

                // From Account
                Cell fromAccountCell = row.createCell(4);
                String fromAccountName = transaction.getFromAccountId() != null
                        ? accountNameMap.getOrDefault(transaction.getFromAccountId(), "")
                        : "";
                fromAccountCell.setCellValue(fromAccountName);
                fromAccountCell.setCellStyle(dataStyle);

                // To Account
                Cell toAccountCell = row.createCell(5);
                String toAccountName = transaction.getToAccountId() != null
                        ? accountNameMap.getOrDefault(transaction.getToAccountId(), "")
                        : "";
                toAccountCell.setCellValue(toAccountName);
                toAccountCell.setCellStyle(dataStyle);

                // Amount (сумма списания)
                Cell amountCell = row.createCell(6);
                if (transaction.getAmount() != null) {
                    amountCell.setCellValue(transaction.getAmount().doubleValue());
                } else {
                    amountCell.setCellValue("");
                }
                amountCell.setCellStyle(dataStyle);

                // Currency
                Cell currencyCell = row.createCell(7);
                currencyCell.setCellValue(transaction.getCurrency() != null ? transaction.getCurrency() : "");
                currencyCell.setCellStyle(dataStyle);

                // Commission (комісія)
                Cell commissionCell = row.createCell(8);
                if (transaction.getCommission() != null && transaction.getCommission().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    commissionCell.setCellValue(transaction.getCommission().doubleValue());
                } else {
                    commissionCell.setCellValue("");
                }
                commissionCell.setCellStyle(dataStyle);

                // Transfer/Received Amount (сумма переказу/зачислення)
                Cell transferAmountCell = row.createCell(9);
                if (transaction.getType() == org.example.userservice.models.transaction.TransactionType.INTERNAL_TRANSFER) {
                    // Для перевода: сумма минус комиссия
                    if (transaction.getAmount() != null) {
                        java.math.BigDecimal transferAmount = transaction.getAmount();
                        if (transaction.getCommission() != null) {
                            transferAmount = transferAmount.subtract(transaction.getCommission());
                        }
                        transferAmountCell.setCellValue(transferAmount.doubleValue());
                    } else {
                        transferAmountCell.setCellValue("");
                    }
                } else if (transaction.getType() == org.example.userservice.models.transaction.TransactionType.CURRENCY_CONVERSION) {
                    // Для конвертации: конвертированная сумма
                    if (transaction.getConvertedAmount() != null) {
                        transferAmountCell.setCellValue(transaction.getConvertedAmount().doubleValue());
                    } else {
                        transferAmountCell.setCellValue("");
                    }
                } else {
                    // Для остальных типов: пусто
                    transferAmountCell.setCellValue("");
                }
                transferAmountCell.setCellStyle(dataStyle);

                // Exchange Rate
                Cell exchangeRateCell = row.createCell(10);
                if (transaction.getExchangeRate() != null) {
                    exchangeRateCell.setCellValue(transaction.getExchangeRate().doubleValue());
                } else {
                    exchangeRateCell.setCellValue("");
                }
                exchangeRateCell.setCellStyle(dataStyle);

                // Converted Currency
                Cell convertedCurrencyCell = row.createCell(11);
                convertedCurrencyCell.setCellValue(
                        transaction.getConvertedCurrency() != null ? transaction.getConvertedCurrency() : "");
                convertedCurrencyCell.setCellStyle(dataStyle);

                // Converted Amount
                Cell convertedAmountCell = row.createCell(12);
                if (transaction.getConvertedAmount() != null) {
                    convertedAmountCell.setCellValue(transaction.getConvertedAmount().doubleValue());
                } else {
                    convertedAmountCell.setCellValue("");
                }
                convertedAmountCell.setCellStyle(dataStyle);

                // Client
                Cell clientCell = row.createCell(13);
                String clientCompany = transaction.getClientId() != null
                        ? clientCompanyMap.getOrDefault(transaction.getClientId(), "")
                        : "";
                clientCell.setCellValue(clientCompany);
                clientCell.setCellStyle(dataStyle);

                // Description
                Cell descriptionCell = row.createCell(14);
                descriptionCell.setCellValue(transaction.getDescription() != null ? transaction.getDescription() : "");
                descriptionCell.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}

