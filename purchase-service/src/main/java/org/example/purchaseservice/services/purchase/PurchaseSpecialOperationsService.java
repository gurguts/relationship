package org.example.purchaseservice.services.purchase;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.clients.*;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.dto.impl.IdNameDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IProductService;
import org.example.purchaseservice.services.impl.IPurchaseSpecialOperationsService;
import org.example.purchaseservice.services.impl.IWarehouseReceiptService;
import org.example.purchaseservice.spec.PurchaseSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSpecialOperationsService implements IPurchaseSpecialOperationsService {

    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final ClientTypeFieldApiClient clientTypeFieldApiClient;
    private final UserClient userClient;
    private final SourceClient sourceClient;
    private final IProductService productService;
    private final IWarehouseReceiptService warehouseReceiptService;

    @Override
    @Transactional(readOnly = true)
    public void generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            HttpServletResponse response,
            List<String> selectedFields) {

        validateInputs(query, selectedFields);
        
        Sort sort = createSort(sortDirection, sortProperty);
        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        FilterIds filterIds = createFilterIds();

        if (clients.isEmpty()) {
            log.info("No clients found for the given filters, returning empty workbook");
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, false);
        
        List<Long> sourceIds = null;
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            sourceIds = fetchSourceIds(query);
        }

        List<Purchase> purchaseList = fetchPurchases(
                query, filterParams, clients.stream().map(ClientDTO::getId).toList(), sourceIds, sort);
        Map<Long, ClientDTO> clientMap = fetchClientMap(clients);
        
        Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap = fetchClientFieldValues(clients.stream().map(ClientDTO::getId).toList());
        
        List<SourceDTO> sourceDTOs = fetchSourceDTOs(purchaseList);
        FilterIds updatedFilterIds = new FilterIds(sourceDTOs, 
                sourceDTOs.stream().map(SourceDTO::getId).toList(),
                filterIds.productDTOs(), filterIds.productIds(),
                filterIds.userDTOs(), filterIds.userIds());

        Workbook workbook = generateWorkbook(purchaseList, selectedFields, updatedFilterIds, clientMap, clientTypeId, clientFieldValuesMap);
        sendExcelFileResponse(workbook, response);
    }

    private record FilterIds(
            List<SourceDTO> sourceDTOs, List<Long> sourceIds,
            List<Product> productDTOs, List<Long> productIds,
            List<UserDTO> userDTOs, List<Long> userIds
    ) {
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private FilterIds createFilterIds() {
        List<Product> products = productService.getAllProducts("all");
        List<Long> productIds = products.stream().map(Product::getId).toList();

        List<UserDTO> userDTOs = userClient.getAllUsers().getBody();
        if (userDTOs == null) {
            userDTOs = Collections.emptyList();
        }
        List<Long> userIds = userDTOs.stream().map(UserDTO::getId).toList();

        return new FilterIds(
                Collections.emptyList(), Collections.emptyList(),
                products, productIds,
                userDTOs, userIds);
    }

    private List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
        
        Map<String, List<String>> filteredParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, true);
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
        List<ClientDTO> clients = clientApiClient.searchClients(clientRequest).getBody();
        return clients != null ? clients : Collections.emptyList();
    }

    private Map<Long, ClientDTO> fetchClientMap(List<ClientDTO> clients) {
        return clients.stream().collect(Collectors.toMap(ClientDTO::getId, client -> client));
    }

    private List<Long> fetchSourceIds(String query) {
        List<SourceDTO> sources = sourceClient.findByNameContaining(query).getBody();
        if (sources == null) {
            sources = Collections.emptyList();
        }
        return sources.stream()
                .map(SourceDTO::getId)
                .toList();
    }

    private List<SourceDTO> fetchSourceDTOs(List<Purchase> purchases) {
        Set<Long> sourceIds = purchases.stream()
                .map(Purchase::getSource)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (sourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SourceDTO> sourceDTOs = new ArrayList<>();
        for (Long sourceId : sourceIds) {
            try {
                SourceDTO sourceDTO = sourceClient.getSourceName(sourceId).getBody();
                if (sourceDTO != null) {
                    sourceDTOs.add(sourceDTO);
                }
            } catch (Exception e) {
                log.warn("Failed to get source name for sourceId {}: {}", sourceId, e.getMessage());
            }
        }

        return sourceDTOs;
    }

    private List<Purchase> fetchPurchases(String query, Map<String, List<String>> filterParams, List<Long> clientIds,
                                          List<Long> sourceIds, Sort sort) {
        Specification<Purchase> spec = (root, querySpec, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            } else {
                return criteriaBuilder.disjunction();
            }

            Specification<Purchase> purchaseSpec = new PurchaseSpecification(query, filterParams, clientIds, sourceIds);
            predicates.add(purchaseSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return purchaseRepository.findAll(spec, sort);
    }

    private Workbook generateWorkbook(List<Purchase> purchaseList, List<String> selectedFields, FilterIds filterIds,
                                      Map<Long, ClientDTO> clientMap, Long clientTypeId, 
                                      Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Data");

        List<String> sortedFields = sortFields(selectedFields);
        Map<String, String> fieldToHeader = createFieldToHeaderMap(sortedFields, clientTypeId);
        createHeaderRow(sheet, sortedFields, fieldToHeader);
        fillDataRows(sheet, purchaseList, sortedFields, filterIds, clientMap, clientFieldValuesMap);

        return workbook;
    }
    
    private List<String> sortFields(List<String> selectedFields) {
        List<String> clientFields = new ArrayList<>();
        List<String> purchaseFields = new ArrayList<>();
        
        for (String field : selectedFields) {
            if (field.endsWith("-client") || field.startsWith("field_")) {
                clientFields.add(field);
            } else {
                purchaseFields.add(field);
            }
        }
        
        List<String> sorted = new ArrayList<>(clientFields);
        sorted.addAll(purchaseFields);
        return sorted;
    }

    private Map<String, String> createFieldToHeaderMap(List<String> selectedFields, Long clientTypeId) {
        Map<String, String> headerMap = new HashMap<>();
        
        headerMap.put("id-client", "Id (клієнта)");
        headerMap.put("company-client", "Компанія (клієнта)");
        headerMap.put("createdAt-client", "Дата створення (клієнта)");
        headerMap.put("updatedAt-client", "Дата оновлення (клієнта)");
        headerMap.put("source-client", "Залучення (клієнта)");
        
        headerMap.put("id", "Id");
        headerMap.put("user", "Водій");
        headerMap.put("source", "Залучення");
        headerMap.put("product", "Товар");
        headerMap.put("quantity", "Кількість");
        headerMap.put("unitPrice", "Ціна за од");
        headerMap.put("totalPrice", "Повна ціна");
        headerMap.put("paymentMethod", "Метод оплати");
        headerMap.put("currency", "Валюта");
        headerMap.put("exchangeRate", "Курс");
        headerMap.put("transaction", "Id транзакції");
        headerMap.put("createdAt", "Дата створення");
        headerMap.put("updatedAt", "Дата оновлення");
        headerMap.put("comment", "Коментар");
        
        List<Long> fieldIds = selectedFields.stream()
                .filter(field -> field.startsWith("field_"))
                .map(field -> {
                    try {
                        return Long.parseLong(field.substring(6));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid field ID in field name {}: {}", field, e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, ClientTypeFieldDTO> fieldMap = new HashMap<>();
        if (!fieldIds.isEmpty()) {
            try {
                List<ClientTypeFieldDTO> fields = clientTypeFieldApiClient.getFieldsByIds(fieldIds).getBody();
                if (fields != null) {
                    fieldMap = fields.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(ClientTypeFieldDTO::getId, field -> field));
                }
            } catch (Exception e) {
                log.warn("Failed to get fields by IDs: {}", e.getMessage());
            }
        }
        
        for (String field : selectedFields) {
            if (field.startsWith("field_")) {
                try {
                    Long fieldId = Long.parseLong(field.substring(6));
                    ClientTypeFieldDTO fieldDTO = fieldMap.get(fieldId);
                    if (fieldDTO != null && fieldDTO.getFieldLabel() != null) {
                        headerMap.put(field, fieldDTO.getFieldLabel() + " (клієнта)");
                    } else {
                        headerMap.put(field, field + " (клієнта)");
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid field ID in field name {}: {}", field, e.getMessage());
                    headerMap.put(field, field + " (клієнта)");
                }
            }
        }
        
        return headerMap;
    }

    private void createHeaderRow(Sheet sheet, List<String> selectedFields, Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            String header = fieldToHeader.getOrDefault(field, field);
            headerRow.createCell(colIndex++).setCellValue(header);
        }
    }

    private void fillDataRows(Sheet sheet, List<Purchase> purchaseList, List<String> selectedFields, FilterIds filterIds,
                              Map<Long, ClientDTO> clientMap, Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        int rowIndex = 1;
        for (Purchase purchase : purchaseList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(purchase.getClient());
            List<ClientFieldValueDTO> fieldValues = client != null ? clientFieldValuesMap.getOrDefault(client.getId(), Collections.emptyList()) : Collections.emptyList();
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(purchase, client, field, filterIds, fieldValues));
            }
        }
    }

    private String getFieldValue(Purchase purchase, ClientDTO client, String field, FilterIds filterIds, 
                                  List<ClientFieldValueDTO> fieldValues) {
        if (field.startsWith("field_")) {
            try {
                Long fieldId = Long.parseLong(field.substring(6));
                return getDynamicFieldValue(fieldValues, fieldId);
            } catch (NumberFormatException e) {
                log.warn("Invalid field ID in field name {}: {}", field, e.getMessage());
                return "";
            }
        }
        
        if (field.endsWith("-client") && client != null) {
            return switch (field) {
                case "id-client" -> client.getId() != null ? String.valueOf(client.getId()) : "";
                case "company-client" -> client.getCompany() != null ? client.getCompany() : "";
                case "createdAt-client" -> client.getCreatedAt() != null ? client.getCreatedAt() : "";
                case "updatedAt-client" -> client.getUpdatedAt() != null ? client.getUpdatedAt() : "";
                case "source-client" -> client.getSourceId() != null ? client.getSourceId() : "";
                default -> "";
            };
        } else {
            return switch (field) {
                case "id" -> purchase.getId() != null ? String.valueOf(purchase.getId()) : "";
                case "user" -> getNameFromDTOList(filterIds.userDTOs(), purchase.getUser());
                case "source" -> getNameFromDTOList(filterIds.sourceDTOs(), purchase.getSource());
                case "product" -> filterIds.productDTOs().stream()
                        .filter(product -> product.getId().equals(purchase.getProduct()))
                        .findFirst()
                        .map(Product::getName)
                        .orElse("");
                case "quantity" -> purchase.getQuantity() != null ? purchase.getQuantity().toString() : "";
                case "unitPrice" -> purchase.getUnitPrice() != null ? purchase.getUnitPrice().toString() : "";
                case "totalPrice" -> purchase.getTotalPrice() != null ? purchase.getTotalPrice().toString() : "";
                case "paymentMethod" -> purchase.getPaymentMethod() != null
                        ? (purchase.getPaymentMethod() == PaymentMethod.CASH ? "2" : "1") : "";
                case "currency" -> purchase.getCurrency() != null ? purchase.getCurrency() : "";
                case "exchangeRate" -> purchase.getExchangeRate() != null ? String.valueOf(purchase.getExchangeRate()) : "";
                case "transaction" -> purchase.getTransaction() != null ? purchase.getTransaction().toString() : "";
                case "createdAt" -> purchase.getCreatedAt() != null ? purchase.getCreatedAt().toString() : "";
                case "updatedAt" -> purchase.getUpdatedAt() != null ? purchase.getUpdatedAt().toString() : "";
                case "comment" -> purchase.getComment() != null ? purchase.getComment() : "";
                default -> "";
            };
        }
    }
    
    private String getDynamicFieldValue(List<ClientFieldValueDTO> fieldValues, Long fieldId) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return "";
        }
        
        List<ClientFieldValueDTO> matchingValues = fieldValues.stream()
                .filter(fv -> fv.getFieldId() != null && fv.getFieldId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .collect(Collectors.toList());
        
        if (matchingValues.isEmpty()) {
            return "";
        }
        
        ClientFieldValueDTO firstValue = matchingValues.get(0);
        String fieldType = firstValue.getFieldType();
        
        if (matchingValues.size() > 1) {
            return matchingValues.stream()
                    .map(fv -> formatFieldValue(fv, fieldType))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
        } else {
            return formatFieldValue(firstValue, fieldType);
        }
    }
    
    private String formatFieldValue(ClientFieldValueDTO fieldValue, String fieldType) {
        if (fieldValue == null) {
            return "";
        }
        
        return switch (fieldType) {
            case "TEXT", "PHONE" -> fieldValue.getValueText() != null ? fieldValue.getValueText() : "";
            case "NUMBER" -> fieldValue.getValueNumber() != null ? fieldValue.getValueNumber().toString() : "";
            case "DATE" -> fieldValue.getValueDate() != null ? fieldValue.getValueDate().toString() : "";
            case "BOOLEAN" -> {
                if (fieldValue.getValueBoolean() == null) yield "";
                yield fieldValue.getValueBoolean() ? "Так" : "Ні";
            }
            case "LIST" -> fieldValue.getValueListValue() != null ? fieldValue.getValueListValue() : "";
            default -> "";
        };
    }
    
    private Map<Long, List<ClientFieldValueDTO>> fetchClientFieldValues(List<Long> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            Map<Long, List<ClientFieldValueDTO>> result = clientApiClient.getClientFieldValuesBatch(clientIds).getBody();
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch field values batch for clients: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private <T extends IdNameDTO> String getNameFromDTOList(List<T> dtoList, Long id) {
        if (id == null) return "";
        return dtoList.stream()
                .filter(dto -> dto.getId().equals(id))
                .findFirst()
                .map(IdNameDTO::getName)
                .orElse("");
    }

    private void validateInputs(String query, List<String> selectedFields) {
        if (query != null && query.length() > 255) {
            throw new PurchaseException("INVALID_QUERY", "Search query cannot exceed 255 characters");
        }
        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new PurchaseException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
    }

    private void sendExcelFileResponse(Workbook workbook, HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = "purchase_data_" + dateStr + ".xlsx";
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            try {
                workbook.close();
            } catch (IOException closeException) {
                log.warn("Failed to close workbook: {}", closeException.getMessage());
            }
            throw new PurchaseException("EXCEL_GENERATION_ERROR", 
                String.format("Error generating Excel file: %s", e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseReportDTO generateReport(String query, Map<String, List<String>> filterParams) {

        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        FilterIds filterIds = createFilterIds();
        
        Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, false);
        
        List<Long> sourceIds = null;
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            sourceIds = fetchSourceIds(query);
        }
        
        List<Purchase> purchaseList = fetchPurchases(query, filterParams, clients, sourceIds);
        
        List<SourceDTO> sourceDTOs = fetchSourceDTOs(purchaseList);
        FilterIds updatedFilterIds = new FilterIds(sourceDTOs, 
                sourceDTOs.stream().map(SourceDTO::getId).toList(),
                filterIds.productDTOs(), filterIds.productIds(),
                filterIds.userDTOs(), filterIds.userIds());
        List<WarehouseReceipt> warehouseReceipts = fetchWarehouseReceipts(filterParams);

        Map<Long, Double> totalCollectedByProduct = calculateTotalCollectedByProduct(purchaseList);
        Map<Long, Double> totalDeliveredByProduct = calculateTotalDeliveredByProduct(warehouseReceipts);
        Map<String, Map<Long, Double>> byDrivers = calculateByDrivers(purchaseList, updatedFilterIds);
        Map<String, Map<Long, Double>> byAttractors = calculateByAttractors(purchaseList, updatedFilterIds);
        Map<String, Double> totalSpentByCurrency = calculateTotalSpentByCurrency(purchaseList);
        Map<String, Double> averagePriceByCurrency = calculateAveragePriceByCurrency(purchaseList);
        Map<Long, Double> averageCollectedPerTimeByProduct = calculateAverageCollectedPerTimeByProduct(purchaseList);

        return buildReport(
                totalCollectedByProduct,
                totalDeliveredByProduct,
                byDrivers,
                byAttractors,
                totalSpentByCurrency,
                averagePriceByCurrency,
                averageCollectedPerTimeByProduct
        );
    }

    private List<Purchase> fetchPurchases(String query, Map<String, List<String>> filterParams,
                                          List<ClientDTO> clients, List<Long> sourceIds) {
        return fetchPurchases(
                query,
                filterParams,
                clients.stream().map(ClientDTO::getId).toList(),
                sourceIds,
                Sort.by("id")
        );
    }

    private List<WarehouseReceipt> fetchWarehouseReceipts(Map<String, List<String>> filterParams) {
        Map<String, List<String>> warehouseFilters = new HashMap<>();
        Map<String, String> filterKeyMapping = new HashMap<>();
        filterKeyMapping.put("user", "user_id");
        filterKeyMapping.put("product", "product_id");
        filterKeyMapping.put("createdAtFrom", "entry_date_from");
        filterKeyMapping.put("createdAtTo", "entry_date_to");

        for (Map.Entry<String, String> entry : filterKeyMapping.entrySet()) {
            String frontendKey = entry.getKey();
            String warehouseKey = entry.getValue();
            if (filterParams.containsKey(frontendKey)) {
                warehouseFilters.put(warehouseKey, filterParams.get(frontendKey));
            }
        }

        return warehouseReceiptService.findWarehouseReceiptsByFilters(warehouseFilters);
    }

    private Map<Long, Double> calculateTotalCollectedByProduct(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getProduct,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Purchase::getQuantity,
                                BigDecimal::add
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    private Map<Long, Double> calculateTotalDeliveredByProduct(List<WarehouseReceipt> warehouseReceipts) {
        return warehouseReceipts.stream()
                .filter(e -> e.getProductId() != null && e.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        WarehouseReceipt::getProductId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                WarehouseReceipt::getQuantity,
                                BigDecimal::add
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    private Map<String, Map<Long, Double>> calculateByDrivers(List<Purchase> purchaseList, FilterIds filterIds) {
        return purchaseList.stream()
                .filter(p -> p.getUser() != null && p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        purchase -> getNameFromDTOList(filterIds.userDTOs(), purchase.getUser()),
                        Collectors.groupingBy(
                                Purchase::getProduct,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Purchase::getQuantity,
                                        BigDecimal::add
                                )
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        v -> v.getValue().doubleValue()
                                ))
                ));
    }

    private Map<String, Map<Long, Double>> calculateByAttractors(List<Purchase> purchaseList, FilterIds filterIds) {
        return purchaseList.stream()
                .filter(p -> p.getSource() != null && p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        purchase -> getNameFromDTOList(filterIds.sourceDTOs(), purchase.getSource()),
                        Collectors.groupingBy(
                                Purchase::getProduct,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Purchase::getQuantity,
                                        BigDecimal::add
                                )
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        v -> v.getValue().doubleValue()
                                ))
                ));
    }

    private Map<String, Double> calculateTotalSpentByCurrency(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getCurrency() != null && p.getTotalPrice() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getCurrency,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Purchase::getTotalPrice,
                                BigDecimal::add
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    private Map<String, Double> calculateAveragePriceByCurrency(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getCurrency() != null && p.getTotalPrice() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getCurrency,
                        Collectors.collectingAndThen(
                                Collectors.averagingDouble(p ->
                                        p.getQuantity().compareTo(BigDecimal.ZERO) > 0
                                                ? p.getTotalPrice().divide(p.getQuantity(), 6, RoundingMode.CEILING).doubleValue()
                                                : 0.0
                                ),
                                avg -> avg
                        )
                ));
    }

    private Map<Long, Double> calculateAverageCollectedPerTimeByProduct(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getProduct,
                        Collectors.collectingAndThen(
                                Collectors.averagingDouble(p -> p.getQuantity().doubleValue()),
                                avg -> avg
                        )
                ));
    }

    private PurchaseReportDTO buildReport(
            Map<Long, Double> totalCollectedByProduct,
            Map<Long, Double> totalDeliveredByProduct,
            Map<String, Map<Long, Double>> byDrivers,
            Map<String, Map<Long, Double>> byAttractors,
            Map<String, Double> totalSpentByCurrency,
            Map<String, Double> averagePriceByCurrency,
            Map<Long, Double> averageCollectedPerTimeByProduct
    ) {
        return PurchaseReportDTO.builder()
                .totalCollectedByProduct(totalCollectedByProduct)
                .totalDeliveredByProduct(totalDeliveredByProduct)
                .byDrivers(byDrivers)
                .byAttractors(byAttractors)
                .totalSpentByCurrency(totalSpentByCurrency)
                .averagePriceByCurrency(averagePriceByCurrency)
                .averageCollectedPerTimeByProduct(averageCollectedPerTimeByProduct)
                .build();
    }


    @Override
    public void generateComparisonExcelFile(String purchaseDataFrom, String purchaseDataTo, HttpServletResponse response) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(purchaseDataFrom, formatter);
            LocalDate toDate = LocalDate.parse(purchaseDataTo, formatter);

            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(23, 59, 59, 999999999);

            ClientSearchRequest request = new ClientSearchRequest(null, null);
            List<ClientDTO> clients = clientApiClient.searchClients(request).getBody();
            if (clients == null) {
                clients = Collections.emptyList();
            }

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Purchase Report");

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Client ID", "Company", "Product", "Total Volume"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;

            Map<Long, String> productNames = productService.getAllProducts("all").stream()
                    .collect(Collectors.toMap(Product::getId, Product::getName));

            List<Long> clientIds = clients.stream()
                    .map(ClientDTO::getId)
                    .collect(Collectors.toList());

            Specification<Purchase> allPurchasesSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (!clientIds.isEmpty()) {
                    predicates.add(root.get("client").in(clientIds));
                }
                predicates.add(cb.between(root.get("createdAt"), fromDateTime, toDateTime));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            List<Purchase> allPurchases = purchaseRepository.findAll(allPurchasesSpec, Pageable.unpaged()).getContent();

            Map<Long, Map<Long, BigDecimal>> clientProductSums = allPurchases.stream()
                    .collect(Collectors.groupingBy(
                            Purchase::getClient,
                            Collectors.groupingBy(
                                    Purchase::getProduct,
                                    Collectors.reducing(
                                            BigDecimal.ZERO,
                                            Purchase::getQuantity,
                                            BigDecimal::add
                                    )
                            )
                    ));

            for (ClientDTO client : clients) {
                Map<Long, BigDecimal> productSums = clientProductSums.getOrDefault(client.getId(), Collections.emptyMap());
                
                if (productSums.isEmpty()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(client.getId());
                    row.createCell(1).setCellValue(client.getCompany());
                    row.createCell(2).setCellValue(productNames.getOrDefault(1L, "Unknown Product"));
                    row.createCell(3).setCellValue(0);
                } else {
                    for (Map.Entry<Long, BigDecimal> entry : productSums.entrySet()) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(client.getId());
                        row.createCell(1).setCellValue(client.getCompany());
                        row.createCell(2).setCellValue(productNames.getOrDefault(entry.getKey(), "Unknown Product"));
                        row.createCell(3).setCellValue(entry.getValue().doubleValue());
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=purchase_report.xlsx");

            workbook.write(response.getOutputStream());
            workbook.close();

        } catch (DateTimeParseException e) {
            log.error("Invalid date format for purchaseDataFrom or purchaseDataTo. Expected format: yyyy-MM-dd", e);
            throw new PurchaseException("INVALID_DATE_FORMAT", "Invalid date format. Please use yyyy-MM-dd");
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            throw new PurchaseException("EXCEL_GENERATION_ERROR", 
                String.format("Failed to generate Excel file: %s", e.getMessage()));
        }
    }
}