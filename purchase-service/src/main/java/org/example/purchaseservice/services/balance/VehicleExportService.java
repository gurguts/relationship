package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.mappers.VehicleMapper;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.repositories.VehicleProductRepository;
import org.example.purchaseservice.repositories.VehicleRepository;
import org.example.purchaseservice.spec.VehicleSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExportService {
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final org.example.purchaseservice.services.balance.VehicleExpenseService vehicleExpenseService;
    private final ProductRepository productRepository;
    private final VehicleProductRepository vehicleProductRepository;
    private final org.example.purchaseservice.clients.TransactionCategoryClient transactionCategoryClient;
    private final org.example.purchaseservice.clients.AccountClient accountClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String SHEET_NAME = "Машини";
    private static final String NO_VEHICLES_MESSAGE = "Машини не знайдено";
    private static final short HEADER_FONT_SIZE = 12;
    private static final String DATE_FORMAT_PATTERN = "dd.mm.yyyy";
    private static final String NUMBER_FORMAT_PATTERN = "#,##0.00";
    private static final String YES = "Так";
    private static final String NO = "Ні";
    private static final String UNKNOWN_PRODUCT = "Невідомий товар";
    private static final String PRODUCT_PREFIX = "Товар #";
    private static final String CATEGORY_PREFIX = "Категорія #";
    private static final String ACCOUNT_PREFIX = "Рахунок #";
    private static final String EXPENSE_PREFIX = "Витрата: ";
    private static final String EUR_SUFFIX = " EUR";
    private static final String KG_SUFFIX = " кг";
    private static final int RECLAMATION_SCALE = 6;
    private static final int DEFAULT_COLUMN_WIDTH = 4000;

    private record ExcelStyles(CellStyle headerStyle, CellStyle dataStyle, CellStyle dateStyle, CellStyle numberStyle) {}

    private record VehicleData(
            List<Vehicle> vehicles,
            Map<Long, Product> productMap,
            Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap,
            Map<Long, String> accountNameMap,
            Map<Long, String> categoryNameMap,
            List<Long> sortedCategoryIds
    ) {}

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] exportToExcel(String query, Map<String, List<String>> filterParams) throws IOException {
        List<Vehicle> vehicles = loadVehicles(query, filterParams);

        if (vehicles.isEmpty()) {
            return createEmptyWorkbook();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            ExcelStyles styles = createExcelStyles(workbook);
            VehicleData vehicleData = loadVehicleData(vehicles);
            
            List<String> headerList = buildHeaderList(vehicleData.sortedCategoryIds(), vehicleData.categoryNameMap());
            createHeaderRow(sheet, headerList, styles.headerStyle());
            
            createDataRows(sheet, vehicles, vehicleData, styles);
            
            setFixedColumnWidths(sheet, headerList.size());
            
            return writeWorkbookToBytes(workbook);
        }
    }

    private List<Vehicle> loadVehicles(String query, Map<String, List<String>> filterParams) {
        VehicleSpecification spec = new VehicleSpecification(query, filterParams);
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        return vehicleRepository.findAll(spec, sortBy);
    }

    private byte[] createEmptyWorkbook() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            Cell headerCell = headerRow.createCell(0);
            headerCell.setCellValue(NO_VEHICLES_MESSAGE);
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

    private VehicleData loadVehicleData(List<Vehicle> vehicles) {
        List<Long> vehicleIds = vehicles.stream()
                .map(Vehicle::getId)
                .toList();
        
        Map<Long, Product> productMap = loadProductMap(vehicleIds);
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap = 
                vehicleExpenseService.getExpensesByVehicleIds(vehicleIds);
        
        ExpenseIds expenseIds = extractExpenseIds(expensesMap);
        Set<Long> categoryIds = expenseIds.categoryIds();
        Set<Long> accountIds = expenseIds.accountIds();
        
        Map<Long, String> accountNameMap = loadAccountNames(accountIds);
        Map<Long, String> categoryNameMap = loadCategoryNames(categoryIds);
        List<Long> sortedCategoryIds = sortCategoryIds(categoryIds, categoryNameMap);
        
        return new VehicleData(vehicles, productMap, expensesMap, accountNameMap, categoryNameMap, sortedCategoryIds);
    }

    private Map<Long, Product> loadProductMap(List<Long> vehicleIds) {
        List<org.example.purchaseservice.models.balance.VehicleProduct> allVehicleProducts = 
                vehicleProductRepository.findByVehicleIdIn(vehicleIds);
        
        List<Long> productIds = allVehicleProducts.stream()
                .map(org.example.purchaseservice.models.balance.VehicleProduct::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Iterable<Product> productsIterable = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : productsIterable) {
            if (product != null && product.getId() != null) {
                productMap.put(product.getId(), product);
            }
        }
        return productMap;
    }

    private ExpenseIds extractExpenseIds(Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap) {
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> accountIds = new HashSet<>();
        
        expensesMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .forEach(expense -> {
                    Long categoryId = expense.getCategoryId();
                    if (categoryId != null) {
                        categoryIds.add(categoryId);
                    }
                    Long accountId = expense.getFromAccountId();
                    if (accountId != null) {
                        accountIds.add(accountId);
                    }
                });
        
        return new ExpenseIds(categoryIds, accountIds);
    }

    private Map<Long, String> loadAccountNames(Set<Long> accountIds) {
        if (isEmpty(accountIds)) {
            return Collections.emptyMap();
        }
        
        Map<Long, String> accountNameMap = new HashMap<>();
        try {
            List<org.example.purchaseservice.models.dto.account.AccountDTO> accounts = accountClient.getAllAccounts().getBody();
            if (accounts != null) {
                accounts.stream()
                        .filter(account -> account != null && account.getId() != null && accountIds.contains(account.getId()))
                        .forEach(account -> accountNameMap.put(account.getId(), account.getName()));
            }
        } catch (Exception e) {
            log.warn("Failed to get account names: {}", e.getMessage());
        }
        return accountNameMap;
    }

    private Map<Long, String> loadCategoryNames(Set<Long> categoryIds) {
        if (isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }
        
        Map<Long, String> categoryNameMap = new HashMap<>();
        List<CompletableFuture<Void>> futures = categoryIds.stream()
                .map(categoryId -> CompletableFuture.runAsync(() -> {
                    try {
                        Map<String, Object> category = transactionCategoryClient.getCategoryById(categoryId).getBody();
                        String name;
                        if (category != null && category.containsKey("name")) {
                            name = (String) category.get("name");
                        } else {
                            name = CATEGORY_PREFIX + categoryId;
                        }
                        synchronized (categoryNameMap) {
                            categoryNameMap.put(categoryId, name);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get category name for categoryId {}: {}", categoryId, e.getMessage());
                        synchronized (categoryNameMap) {
                            categoryNameMap.put(categoryId, CATEGORY_PREFIX + categoryId);
                        }
                    }
                }))
                .toList();
        
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Error loading category names: {}", e.getMessage());
        }
        
        return categoryNameMap;
    }

    private List<Long> sortCategoryIds(Set<Long> categoryIds, Map<Long, String> categoryNameMap) {
        return categoryIds.stream()
                .sorted((id1, id2) -> {
                    String name1 = categoryNameMap.getOrDefault(id1, "");
                    String name2 = categoryNameMap.getOrDefault(id2, "");
                    return name1.compareToIgnoreCase(name2);
                })
                .toList();
    }

    private List<String> buildHeaderList(List<Long> sortedCategoryIds, Map<Long, String> categoryNameMap) {
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
            headerList.add(EXPENSE_PREFIX + categoryName);
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
        
        return headerList;
    }

    private void createHeaderRow(Sheet sheet, List<String> headerList, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headerList.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerList.get(i));
            cell.setCellStyle(headerStyle);
        }
    }

    private void createDataRows(Sheet sheet, List<Vehicle> vehicles, VehicleData vehicleData, ExcelStyles styles) {
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
                                 VehicleData vehicleData, ExcelStyles styles) {
        int columnIndex = 0;
        
        VehicleProductsData productsData = calculateVehicleProductsData(vehicleDTO, vehicleData.productMap());
        VehicleExpensesData expensesData = calculateVehicleExpensesData(expenses);
        FinancialMetrics financialMetrics = calculateFinancialMetrics(productsData, expensesData, vehicleDTO);
        
        columnIndex = writeFinancialColumns(row, columnIndex, productsData, expensesData, styles);
        columnIndex = writeInvoiceColumns(row, columnIndex, vehicleDTO, styles);
        columnIndex = writeReclamationColumns(row, columnIndex, financialMetrics.reclamationData(), styles);
        columnIndex = writeSummaryColumns(row, columnIndex, financialMetrics, styles);
        columnIndex = writeProductsColumn(row, columnIndex, productsData, styles);
        columnIndex = writeExpenseCategoryColumns(row, columnIndex, expenses, vehicleData, styles);
        columnIndex = writeVehicleInfoColumns(row, columnIndex, vehicleDTO, styles);
        writeCarrierColumns(row, columnIndex, vehicleDTO, styles);
    }

    private VehicleProductsData calculateVehicleProductsData(VehicleDetailsDTO vehicleDTO, Map<Long, Product> productMap) {
        StringBuilder productsText = new StringBuilder();
        BigDecimal productsTotalCost = BigDecimal.ZERO;
        
        if (isNotEmpty(vehicleDTO.getItems())) {
            for (VehicleDetailsDTO.VehicleItemDTO item : vehicleDTO.getItems()) {
                if (!productsText.isEmpty()) {
                    productsText.append("\n");
                }
                String productName = getProductName(item.getProductId(), productMap);
                BigDecimal itemTotalCost = safeGetBigDecimal(item.getTotalCostEur());
                productsTotalCost = productsTotalCost.add(itemTotalCost);
                productsText.append(String.format("%s, Кількість: %s%s, Ціна: %s%s, Сума: %s%s",
                        productName,
                        formatNumber(item.getQuantity()),
                        KG_SUFFIX,
                        formatNumber(item.getUnitPriceEur()),
                        EUR_SUFFIX,
                        formatNumber(item.getTotalCostEur()),
                        EUR_SUFFIX));
            }
        }
        
        return new VehicleProductsData(productsText.toString(), productsTotalCost);
    }

    private VehicleExpensesData calculateVehicleExpensesData(List<org.example.purchaseservice.models.balance.VehicleExpense> expenses) {
        BigDecimal expensesTotal = BigDecimal.ZERO;
        if (isNotEmpty(expenses)) {
            for (org.example.purchaseservice.models.balance.VehicleExpense expense : expenses) {
                BigDecimal convertedAmount = expense.getConvertedAmount();
                if (convertedAmount != null) {
                    expensesTotal = expensesTotal.add(convertedAmount);
                }
            }
        }
        return new VehicleExpensesData(expensesTotal);
    }

    private FinancialMetrics calculateFinancialMetrics(VehicleProductsData productsData, 
                                                       VehicleExpensesData expensesData,
                                                       VehicleDetailsDTO vehicleDTO) {
        BigDecimal totalExpenses = productsData.totalCost().add(expensesData.total());
        BigDecimal invoiceEuTotalPrice = safeGetBigDecimal(vehicleDTO.getInvoiceEuTotalPrice());
        ReclamationData reclamationData = calculateReclamation(vehicleDTO);
        BigDecimal totalIncome = invoiceEuTotalPrice.subtract(reclamationData.fullReclamation());
        BigDecimal margin = totalIncome.subtract(totalExpenses);
        
        return new FinancialMetrics(totalExpenses, totalIncome, margin, reclamationData);
    }

    private ReclamationData calculateReclamation(VehicleDetailsDTO vehicleDTO) {
        BigDecimal reclamationPerTon = safeGetBigDecimal(vehicleDTO.getReclamation());
        BigDecimal fullReclamation = BigDecimal.ZERO;
        
        if (reclamationPerTon.compareTo(BigDecimal.ZERO) > 0 && isNotEmpty(vehicleDTO.getProductQuantity())) {
            try {
                BigDecimal quantityInTons = new BigDecimal(vehicleDTO.getProductQuantity().replace(",", "."));
                fullReclamation = reclamationPerTon.multiply(quantityInTons)
                        .setScale(RECLAMATION_SCALE, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse productQuantity for reclamation calculation: {}", vehicleDTO.getProductQuantity(), e);
            }
        }
        
        return new ReclamationData(reclamationPerTon, fullReclamation);
    }

    private int writeFinancialColumns(Row row, int columnIndex, VehicleProductsData productsData,
                                     VehicleExpensesData expensesData,
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

    private int writeReclamationColumns(Row row, int columnIndex, ReclamationData reclamationData, ExcelStyles styles) {
        setCellValue(row, columnIndex++, reclamationData.reclamationPerTon(), styles.numberStyle());
        setCellValue(row, columnIndex++, reclamationData.fullReclamation(), styles.numberStyle());
        return columnIndex;
    }

    private int writeSummaryColumns(Row row, int columnIndex, FinancialMetrics financialMetrics, ExcelStyles styles) {
        setCellValue(row, columnIndex++, financialMetrics.totalExpenses(), styles.numberStyle());
        setCellValue(row, columnIndex++, financialMetrics.totalIncome(), styles.numberStyle());
        setCellValue(row, columnIndex++, financialMetrics.margin(), styles.numberStyle());
        return columnIndex;
    }

    private int writeProductsColumn(Row row, int columnIndex, VehicleProductsData productsData, ExcelStyles styles) {
        setCellValue(row, columnIndex++, productsData.productsText(), styles.dataStyle());
        return columnIndex;
    }

    private int writeExpenseCategoryColumns(Row row, int columnIndex,
                                           List<org.example.purchaseservice.models.balance.VehicleExpense> expenses,
                                           VehicleData vehicleData, ExcelStyles styles) {
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesByCategoryMap = 
                groupExpensesByCategory(expenses);
        
        for (Long categoryId : vehicleData.sortedCategoryIds()) {
            List<org.example.purchaseservice.models.balance.VehicleExpense> categoryExpenses = 
                    expensesByCategoryMap.getOrDefault(categoryId, Collections.emptyList());
            String expenseDetails = formatExpenseDetails(categoryExpenses, vehicleData.accountNameMap());
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

    private String formatExpenseDetails(List<org.example.purchaseservice.models.balance.VehicleExpense> categoryExpenses,
                                       Map<Long, String> accountNameMap) {
        if (isEmpty(categoryExpenses)) {
            return "";
        }
        
        StringBuilder expenseDetails = new StringBuilder();
        for (org.example.purchaseservice.models.balance.VehicleExpense expense : categoryExpenses) {
            if (!expenseDetails.isEmpty()) {
                expenseDetails.append("\n");
            }
            
            String accountName = getAccountName(expense.getFromAccountId(), accountNameMap);
            String currency = safeString(expense.getCurrency());
            String exchangeRate = safeFormatNumber(expense.getExchangeRate());
            String description = safeString(expense.getDescription());
            String amountEur = safeFormatNumber(expense.getConvertedAmount()) + EUR_SUFFIX;
            String originalAmount = safeFormatNumber(expense.getAmount());
            
            expenseDetails.append(String.format("%s %s (курс: %s) = %s", 
                    originalAmount, currency, exchangeRate, amountEur));
            
            if (isNotEmpty(accountName)) {
                expenseDetails.append(", Рахунок: ").append(accountName);
            }
            if (isNotEmpty(description)) {
                expenseDetails.append(", Опис: ").append(description);
            }
        }
        return expenseDetails.toString();
    }

    private String getAccountName(Long accountId, Map<Long, String> accountNameMap) {
        if (accountId == null) {
            return "";
        }
        return accountNameMap.getOrDefault(accountId, ACCOUNT_PREFIX + accountId);
    }

    private int writeVehicleInfoColumns(Row row, int columnIndex, VehicleDetailsDTO vehicleDTO, ExcelStyles styles) {
        setCellValue(row, columnIndex++, vehicleDTO.getId(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getShipmentDate(), styles.dateStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getVehicleNumber(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDescription(), styles.dataStyle());
        setCellValue(row, columnIndex++, formatBoolean(vehicleDTO.getIsOurVehicle()), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getSenderName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getReceiverName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getTerminalName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDestinationCountryName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDestinationPlaceName(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDeclarationNumber(), styles.dataStyle());
        setCellValue(row, columnIndex++, vehicleDTO.getDriverFullName(), styles.dataStyle());
        setCellValue(row, columnIndex++, formatBoolean(vehicleDTO.getEur1()), styles.dataStyle());
        setCellValue(row, columnIndex++, formatBoolean(vehicleDTO.getFito()), styles.dataStyle());
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
            case java.time.LocalDate localDate -> cell.setCellValue(localDate.format(DATE_FORMATTER));
            default -> cell.setCellValue(value.toString());
        }
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) return "";
        return String.format("%.2f", value);
    }

    private String getProductName(Long productId, Map<Long, Product> productMap) {
        if (productId == null) {
            return UNKNOWN_PRODUCT;
        }
        Product product = productMap.get(productId);
        return product != null ? product.getName() : PRODUCT_PREFIX + productId;
    }

    private boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private BigDecimal safeGetBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String safeFormatNumber(BigDecimal value) {
        return value != null ? formatNumber(value) : "";
    }

    private String formatBoolean(Boolean value) {
        return value != null && value ? YES : NO;
    }

    private record VehicleProductsData(String productsText, BigDecimal totalCost) {}
    private record VehicleExpensesData(BigDecimal total) {}
    private record FinancialMetrics(BigDecimal totalExpenses, BigDecimal totalIncome, BigDecimal margin, ReclamationData reclamationData) {}
    private record ReclamationData(BigDecimal reclamationPerTon, BigDecimal fullReclamation) {}
    private record ExpenseIds(Set<Long> categoryIds, Set<Long> accountIds) {}
}
