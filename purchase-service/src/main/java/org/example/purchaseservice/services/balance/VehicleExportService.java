package org.example.purchaseservice.services.balance;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.clients.TransactionApiClient;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExportService {
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final TransactionApiClient transactionApiClient;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final VehicleProductRepository vehicleProductRepository;

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
            String[] headers = {
                    "ID", "Дата відвантаження", "Номер машини", "Наше завантаження",
                    "Відправник", "Отримувач", "Країна призначення", "Місце призначення",
                    "Товар", "Кількість товару", "Номер декларації", "Термінал",
                    "Водій (ПІБ)", "EUR1", "FITO", "Дата митниці", "Дата розмитнення",
                    "Дата вивантаження", "Перевізник", "Товари зі складу", "Витрати на машину", "Загальна вартість (EUR)"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

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
            
            Map<Long, List<Map<String, Object>>> transactionsMap = getVehicleTransactionsBatch(vehicleIds);
            
            int rowNum = 1;
            for (Vehicle vehicle : vehicles) {
                VehicleDetailsDTO vehicleDTO = vehicleService.mapToDetailsDTO(vehicle);
                List<Map<String, Object>> transactions = transactionsMap.getOrDefault(vehicle.getId(), Collections.emptyList());

                Row mainRow = sheet.createRow(rowNum++);

                setCellValue(mainRow, 0, vehicleDTO.getId(), dataStyle);
                setCellValue(mainRow, 1, vehicleDTO.getShipmentDate(), dateStyle);
                setCellValue(mainRow, 2, vehicleDTO.getVehicleNumber(), dataStyle);
                setCellValue(mainRow, 3, vehicleDTO.getIsOurVehicle() != null && vehicleDTO.getIsOurVehicle() ? "Так" : "Ні", dataStyle);
                setCellValue(mainRow, 4, vehicleDTO.getSender(), dataStyle);
                setCellValue(mainRow, 5, vehicleDTO.getReceiver(), dataStyle);
                setCellValue(mainRow, 6, vehicleDTO.getDestinationCountry(), dataStyle);
                setCellValue(mainRow, 7, vehicleDTO.getDestinationPlace(), dataStyle);
                setCellValue(mainRow, 8, vehicleDTO.getProduct(), dataStyle);
                setCellValue(mainRow, 9, vehicleDTO.getProductQuantity(), dataStyle);
                setCellValue(mainRow, 10, vehicleDTO.getDeclarationNumber(), dataStyle);
                setCellValue(mainRow, 11, vehicleDTO.getTerminal(), dataStyle);
                setCellValue(mainRow, 12, vehicleDTO.getDriverFullName(), dataStyle);
                setCellValue(mainRow, 13, vehicleDTO.getEur1() != null && vehicleDTO.getEur1() ? "Так" : "Ні", dataStyle);
                setCellValue(mainRow, 14, vehicleDTO.getFito() != null && vehicleDTO.getFito() ? "Так" : "Ні", dataStyle);
                setCellValue(mainRow, 15, vehicleDTO.getCustomsDate(), dateStyle);
                setCellValue(mainRow, 16, vehicleDTO.getCustomsClearanceDate(), dateStyle);
                setCellValue(mainRow, 17, vehicleDTO.getUnloadingDate(), dateStyle);
                setCellValue(mainRow, 18, vehicleDTO.getCarrier() != null ? vehicleDTO.getCarrier().getCompanyName() : "", dataStyle);

                StringBuilder productsText = new StringBuilder();
                if (vehicleDTO.getItems() != null && !vehicleDTO.getItems().isEmpty()) {
                    for (VehicleDetailsDTO.VehicleItemDTO item : vehicleDTO.getItems()) {
                        if (productsText.length() > 0) productsText.append("\n");
                        String productName = getProductName(item.getProductId(), productMap);
                        productsText.append(String.format("%s, Кількість: %s кг, Ціна: %s EUR, Сума: %s EUR",
                                productName,
                                formatNumber(item.getQuantity()),
                                formatNumber(item.getUnitPriceEur()),
                                formatNumber(item.getTotalCostEur())));
                    }
                }
                setCellValue(mainRow, 19, productsText.toString(), dataStyle);

                StringBuilder expensesText = new StringBuilder();
                BigDecimal totalExpenses = BigDecimal.ZERO;
                if (transactions != null && !transactions.isEmpty()) {
                    for (Map<String, Object> transaction : transactions) {
                        if (expensesText.length() > 0) expensesText.append("\n");
                        BigDecimal amount = getBigDecimal(transaction.get("amount"));
                        String currency = (String) transaction.get("currency");
                        BigDecimal exchangeRate = getBigDecimal(transaction.get("exchangeRate"));
                        BigDecimal convertedAmount = getBigDecimal(transaction.get("convertedAmount"));
                        String description = (String) transaction.get("description");
                        if (convertedAmount != null) {
                            totalExpenses = totalExpenses.add(convertedAmount);
                        }
                        expensesText.append(String.format("%s %s (курс: %s) = %s EUR%s",
                                formatNumber(amount),
                                currency != null ? currency : "",
                                exchangeRate != null ? formatNumber(exchangeRate) : "",
                                convertedAmount != null ? formatNumber(convertedAmount) : "",
                                description != null && !description.isEmpty() ? " - " + description : ""));
                    }
                }
                setCellValue(mainRow, 20, expensesText.toString(), dataStyle);
                setCellValue(mainRow, 21, vehicleDTO.getTotalCostEur(), numberStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<Map<String, Object>>> getVehicleTransactionsBatch(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<Long, List<?>> transactionsMap = transactionApiClient.getTransactionsByVehicleIds(vehicleIds);
            return transactionsMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().stream()
                                    .map(t -> objectMapper.convertValue(t, Map.class))
                                    .map(m -> (Map<String, Object>) m)
                                    .toList()
                    ));
        } catch (Exception e) {
            log.warn("Failed to get transactions batch for vehicles: {}", e.getMessage());
            return Collections.emptyMap();
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

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getProductName(Long productId, Map<Long, Product> productMap) {
        if (productId == null) {
            return "Невідомий товар";
        }
        Product product = productMap.get(productId);
        return product != null ? product.getName() : "Товар #" + productId;
    }
}

