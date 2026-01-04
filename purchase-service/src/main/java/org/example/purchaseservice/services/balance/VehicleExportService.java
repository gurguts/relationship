package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExportService {
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final org.example.purchaseservice.services.balance.VehicleExpenseService vehicleExpenseService;
    private final ProductRepository productRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final org.example.purchaseservice.clients.TransactionCategoryClient transactionCategoryClient;
    private final org.example.purchaseservice.clients.AccountClient accountClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] exportToExcel(String query, Map<String, List<String>> filterParams) throws IOException {
        VehicleSpecification spec = new VehicleSpecification(query, filterParams);
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Vehicle> vehicles = vehicleRepository.findAll(spec, sortBy);

        if (vehicles.isEmpty()) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Машини");
                Row headerRow = sheet.createRow(0);
                Cell headerCell = headerRow.createCell(0);
                headerCell.setCellValue("Машини не знайдено");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Машини");

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
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(dataStyle);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(dataStyle);
            numberStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            Row headerRow = sheet.createRow(0);

            List<Long> vehicleIds = vehicles.stream()
                    .map(Vehicle::getId)
                    .collect(Collectors.toList());
            
            List<org.example.purchaseservice.models.balance.VehicleProduct> allVehicleProducts = 
                    vehicleProductRepository.findByVehicleIdIn(vehicleIds);
            
            List<Long> productIds = allVehicleProducts.stream()
                    .map(org.example.purchaseservice.models.balance.VehicleProduct::getProductId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            Map<Long, Product> productMap = productIds.isEmpty() ? 
                    Collections.emptyMap() : 
                    ((List<Product>) productRepository.findAllById(productIds)).stream()
                            .collect(Collectors.toMap(Product::getId, product -> product));
            
            Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap = vehicleExpenseService.getExpensesByVehicleIds(vehicleIds);
            
            Set<Long> categoryIds = expensesMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(org.example.purchaseservice.models.balance.VehicleExpense::getCategoryId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            Set<Long> accountIds = expensesMap.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .map(org.example.purchaseservice.models.balance.VehicleExpense::getFromAccountId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            
            Map<Long, String> accountNameMap = new HashMap<>();
            if (!accountIds.isEmpty()) {
                try {
                    List<org.example.purchaseservice.models.dto.account.AccountDTO> accounts = accountClient.getAllAccounts().getBody();
                    if (accounts == null) {
                        accounts = Collections.emptyList();
                    }
                    for (org.example.purchaseservice.models.dto.account.AccountDTO account : accounts) {
                        if (accountIds.contains(account.getId())) {
                            accountNameMap.put(account.getId(), account.getName());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get account names: {}", e.getMessage());
                }
            }
            
            Map<Long, String> categoryNameMap = new HashMap<>();
            for (Long categoryId : categoryIds) {
                try {
                    Map<String, Object> category = transactionCategoryClient.getCategoryById(categoryId).getBody();
                    if (category != null && category.containsKey("name")) {
                        categoryNameMap.put(categoryId, (String) category.get("name"));
                    } else {
                        categoryNameMap.put(categoryId, "Категорія #" + categoryId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get category name for categoryId {}: {}", categoryId, e.getMessage());
                    categoryNameMap.put(categoryId, "Категорія #" + categoryId);
                }
            }
            
            List<Long> sortedCategoryIds = categoryIds.stream()
                    .sorted((id1, id2) -> {
                        String name1 = categoryNameMap.getOrDefault(id1, "");
                        String name2 = categoryNameMap.getOrDefault(id2, "");
                        return name1.compareToIgnoreCase(name2);
                    })
                    .collect(Collectors.toList());
            
            List<String> headerList = new ArrayList<>();
            headerList.add("Сума товарів зі складу (EUR)");
            headerList.add("Сума витрат на машину (EUR)");
            headerList.add("Товар");
            headerList.add("Кількість товару");
            headerList.add("Інвойс UA");
            headerList.add("Дата інвойсу UA");
            headerList.add("Ціна за тонну інвойсу UA");
            headerList.add("Повна ціна інвойсу UA");
            headerList.add("Інвойс EU");
            headerList.add("Дата інвойсу EU");
            headerList.add("Ціна за тонну інвойсу EU");
            headerList.add("Повна ціна інвойсу EU");
            headerList.add("Рекламація за т");
            headerList.add("Повна рекламація");
            headerList.add("Загальні витрати (EUR)");
            headerList.add("Загальний дохід (EUR)");
            headerList.add("Маржа");
            headerList.add("Товари зі складу");
            for (Long categoryId : sortedCategoryIds) {
                String categoryName = categoryNameMap.get(categoryId);
                headerList.add("Витрата: " + categoryName);
            }
            headerList.add("ID");
            headerList.add("Дата відвантаження");
            headerList.add("Номер машини");
            headerList.add("Опис");
            headerList.add("Наше завантаження");
            headerList.add("Відправник");
            headerList.add("Отримувач");
            headerList.add("Країна призначення");
            headerList.add("Місце призначення");
            headerList.add("Номер декларації");
            headerList.add("Термінал");
            headerList.add("Водій (ПІБ)");
            headerList.add("EUR1");
            headerList.add("FITO");
            headerList.add("Дата замитнення");
            headerList.add("Дата розмитнення");
            headerList.add("Дата вивантаження");
            headerList.add("Перевізник (назва)");
            headerList.add("Перевізник (адреса)");
            headerList.add("Перевізник (телефон)");
            headerList.add("Перевізник (код)");
            headerList.add("Перевізник (рахунок)");
            
            for (int i = 0; i < headerList.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerList.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            int rowNum = 1;
            for (Vehicle vehicle : vehicles) {
                VehicleDetailsDTO vehicleDTO = vehicleService.mapToDetailsDTO(vehicle);
                List<org.example.purchaseservice.models.balance.VehicleExpense> expenses = expensesMap.getOrDefault(vehicle.getId(), Collections.emptyList());

                Row mainRow = sheet.createRow(rowNum++);

                int col = 0;

                StringBuilder productsText = new StringBuilder();
                BigDecimal productsTotalCost = BigDecimal.ZERO;
                if (vehicleDTO.getItems() != null && !vehicleDTO.getItems().isEmpty()) {
                    for (VehicleDetailsDTO.VehicleItemDTO item : vehicleDTO.getItems()) {
                        if (productsText.length() > 0) productsText.append("\n");
                        String productName = getProductName(item.getProductId(), productMap);
                        BigDecimal itemTotalCost = item.getTotalCostEur() != null ? item.getTotalCostEur() : BigDecimal.ZERO;
                        productsTotalCost = productsTotalCost.add(itemTotalCost);
                        productsText.append(String.format("%s, Кількість: %s кг, Ціна: %s EUR, Сума: %s EUR",
                                productName,
                                formatNumber(item.getQuantity()),
                                formatNumber(item.getUnitPriceEur()),
                                formatNumber(item.getTotalCostEur())));
                    }
                }
                BigDecimal expensesTotal = BigDecimal.ZERO;
                if (expenses != null && !expenses.isEmpty()) {
                    for (org.example.purchaseservice.models.balance.VehicleExpense expense : expenses) {
                        BigDecimal convertedAmount = expense.getConvertedAmount();
                        if (convertedAmount != null) {
                            expensesTotal = expensesTotal.add(convertedAmount);
                        }
                    }
                }
                
                setCellValue(mainRow, col++, productsTotalCost, numberStyle);
                setCellValue(mainRow, col++, expensesTotal, numberStyle);

                setCellValue(mainRow, col++, vehicleDTO.getProduct(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getProductQuantity(), dataStyle);

                setCellValue(mainRow, col++, vehicleDTO.getInvoiceUa(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getInvoiceUaDate(), dateStyle);
                setCellValue(mainRow, col++, vehicleDTO.getInvoiceUaPricePerTon(), numberStyle);
                setCellValue(mainRow, col++, vehicleDTO.getInvoiceUaTotalPrice(), numberStyle);

                setCellValue(mainRow, col++, vehicleDTO.getInvoiceEu(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getInvoiceEuDate(), dateStyle);
                setCellValue(mainRow, col++, vehicleDTO.getInvoiceEuPricePerTon(), numberStyle);
                setCellValue(mainRow, col++, vehicleDTO.getInvoiceEuTotalPrice(), numberStyle);
                BigDecimal reclamationPerTon = vehicleDTO.getReclamation() != null ? vehicleDTO.getReclamation() : BigDecimal.ZERO;
                BigDecimal fullReclamation = BigDecimal.ZERO;
                if (reclamationPerTon.compareTo(BigDecimal.ZERO) > 0 && vehicleDTO.getProductQuantity() != null && !vehicleDTO.getProductQuantity().trim().isEmpty()) {
                    try {
                        BigDecimal quantityInTons = new BigDecimal(vehicleDTO.getProductQuantity().replace(",", "."));
                        fullReclamation = reclamationPerTon.multiply(quantityInTons).setScale(6, java.math.RoundingMode.HALF_UP);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse productQuantity for reclamation calculation: {}", vehicleDTO.getProductQuantity(), e);
                    }
                }
                setCellValue(mainRow, col++, reclamationPerTon, numberStyle);
                setCellValue(mainRow, col++, fullReclamation, numberStyle);

                BigDecimal totalExpenses = productsTotalCost.add(expensesTotal);
                BigDecimal invoiceEuTotalPrice = vehicleDTO.getInvoiceEuTotalPrice() != null ? vehicleDTO.getInvoiceEuTotalPrice() : BigDecimal.ZERO;
                BigDecimal totalIncome = invoiceEuTotalPrice.subtract(fullReclamation);
                BigDecimal margin = totalIncome.subtract(totalExpenses);

                setCellValue(mainRow, col++, totalExpenses, numberStyle);
                setCellValue(mainRow, col++, totalIncome, numberStyle);
                setCellValue(mainRow, col++, margin, numberStyle);

                setCellValue(mainRow, col++, productsText.toString(), dataStyle);
                
                Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesByCategoryMap = new HashMap<>();
                if (expenses != null && !expenses.isEmpty()) {
                    for (org.example.purchaseservice.models.balance.VehicleExpense expense : expenses) {
                        Long categoryId = expense.getCategoryId();
                        if (categoryId != null) {
                            expensesByCategoryMap.computeIfAbsent(categoryId, k -> new ArrayList<>()).add(expense);
                        }
                    }
                }
                
                for (Long categoryId : sortedCategoryIds) {
                    List<org.example.purchaseservice.models.balance.VehicleExpense> categoryExpenses = expensesByCategoryMap.getOrDefault(categoryId, Collections.emptyList());
                    StringBuilder expenseDetails = new StringBuilder();
                    if (!categoryExpenses.isEmpty()) {
                        for (org.example.purchaseservice.models.balance.VehicleExpense expense : categoryExpenses) {
                            if (expenseDetails.length() > 0) expenseDetails.append("\n");
                            
                            String accountName = "";
                            if (expense.getFromAccountId() != null) {
                                accountName = accountNameMap.getOrDefault(expense.getFromAccountId(), "Рахунок #" + expense.getFromAccountId());
                            }
                            
                            String currency = expense.getCurrency() != null ? expense.getCurrency() : "";
                            String exchangeRate = expense.getExchangeRate() != null ? formatNumber(expense.getExchangeRate()) : "";
                            String description = expense.getDescription() != null && !expense.getDescription().isEmpty() ? expense.getDescription() : "";
                            String amountEur = expense.getConvertedAmount() != null ? formatNumber(expense.getConvertedAmount()) + " EUR" : "";
                            String originalAmount = expense.getAmount() != null ? formatNumber(expense.getAmount()) : "";
                            
                            expenseDetails.append(String.format("%s %s (курс: %s) = %s", 
                                    originalAmount, currency, exchangeRate, amountEur));
                            if (!accountName.isEmpty()) {
                                expenseDetails.append(", Рахунок: ").append(accountName);
                            }
                            if (!description.isEmpty()) {
                                expenseDetails.append(", Опис: ").append(description);
                            }
                        }
                    }
                    setCellValue(mainRow, col++, expenseDetails.toString(), dataStyle);
                }

                setCellValue(mainRow, col++, vehicleDTO.getId(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getShipmentDate(), dateStyle);
                setCellValue(mainRow, col++, vehicleDTO.getVehicleNumber(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getDescription(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getIsOurVehicle() != null && vehicleDTO.getIsOurVehicle() ? "Так" : "Ні", dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getSenderName(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getReceiverName(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getDestinationCountry(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getDestinationPlace(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getDeclarationNumber(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getTerminal(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getDriverFullName(), dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getEur1() != null && vehicleDTO.getEur1() ? "Так" : "Ні", dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getFito() != null && vehicleDTO.getFito() ? "Так" : "Ні", dataStyle);
                setCellValue(mainRow, col++, vehicleDTO.getCustomsDate(), dateStyle);
                setCellValue(mainRow, col++, vehicleDTO.getCustomsClearanceDate(), dateStyle);
                setCellValue(mainRow, col++, vehicleDTO.getUnloadingDate(), dateStyle);
                
                if (vehicleDTO.getCarrier() != null) {
                    setCellValue(mainRow, col++, vehicleDTO.getCarrier().getCompanyName(), dataStyle);
                    setCellValue(mainRow, col++, vehicleDTO.getCarrier().getRegistrationAddress(), dataStyle);
                    setCellValue(mainRow, col++, vehicleDTO.getCarrier().getPhoneNumber(), dataStyle);
                    setCellValue(mainRow, col++, vehicleDTO.getCarrier().getCode(), dataStyle);
                    setCellValue(mainRow, col++, vehicleDTO.getCarrier().getAccount(), dataStyle);
                } else {
                    setCellValue(mainRow, col++, "", dataStyle);
                    setCellValue(mainRow, col++, "", dataStyle);
                    setCellValue(mainRow, col++, "", dataStyle);
                    setCellValue(mainRow, col++, "", dataStyle);
                    setCellValue(mainRow, col++, "", dataStyle);
                }
            }

            for (int i = 0; i < headerList.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void setCellValue(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof java.time.LocalDate) {
            cell.setCellValue(((java.time.LocalDate) value).format(DATE_FORMATTER));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) return "";
        return String.format("%.2f", value);
    }

    private String getProductName(Long productId, Map<Long, Product> productMap) {
        if (productId == null) {
            return "Невідомий товар";
        }
        Product product = productMap.get(productId);
        return product != null ? product.getName() : "Товар #" + productId;
    }
}

