package org.example.purchaseservice.services.purchase;

import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.clients.*;
import org.example.purchaseservice.services.source.SourceService;
import org.example.purchaseservice.services.user.UserService;
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

    private static final int MAX_QUERY_LENGTH = 255;
    private static final int PRICE_SCALE = 6;
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String FIELD_PREFIX = "field_";
    private static final String CLIENT_SUFFIX = "-client";
    private static final String UNKNOWN_PRODUCT = "Unknown Product";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EXCEL_FILENAME_PREFIX = "purchase_data_";
    private static final String EXCEL_FILENAME_SUFFIX = ".xlsx";
    private static final String COMPARISON_EXCEL_FILENAME = "purchase_report.xlsx";
    private static final Long DEFAULT_PRODUCT_ID = 1L;
    private static final RoundingMode PRICE_ROUNDING_MODE = RoundingMode.CEILING;
    
    private static final String HEADER_ID_CLIENT = "Id (клієнта)";
    private static final String HEADER_COMPANY_CLIENT = "Компанія (клієнта)";
    private static final String HEADER_CREATED_AT_CLIENT = "Дата створення (клієнта)";
    private static final String HEADER_UPDATED_AT_CLIENT = "Дата оновлення (клієнта)";
    private static final String HEADER_SOURCE_CLIENT = "Залучення (клієнта)";
    private static final String HEADER_ID = "Id";
    private static final String HEADER_USER = "Водій";
    private static final String HEADER_SOURCE = "Залучення";
    private static final String HEADER_PRODUCT = "Товар";
    private static final String HEADER_QUANTITY = "Кількість";
    private static final String HEADER_UNIT_PRICE = "Ціна за од";
    private static final String HEADER_TOTAL_PRICE = "Повна ціна";
    private static final String HEADER_PAYMENT_METHOD = "Метод оплати";
    private static final String HEADER_CURRENCY = "Валюта";
    private static final String HEADER_EXCHANGE_RATE = "Курс";
    private static final String HEADER_TRANSACTION = "Id транзакції";
    private static final String HEADER_CREATED_AT = "Дата створення";
    private static final String HEADER_UPDATED_AT = "Дата оновлення";
    private static final String HEADER_COMMENT = "Коментар";
    private static final String HEADER_CLIENT_SUFFIX = " (клієнта)";
    
    private static final Set<String> VALID_PURCHASE_FIELDS = Set.of(
            "id", "user", "source", "product", "quantity", "unitPrice", "totalPrice",
            "paymentMethod", "currency", "exchangeRate", "transaction", "createdAt", "updatedAt", "comment"
    );
    
    private static final Set<String> VALID_CLIENT_FIELDS = Set.of(
            "id-client", "company-client", "createdAt-client", "updatedAt-client", "source-client"
    );
    
    private static final String ALL_PRODUCTS_FILTER = "all";
    
    private static final Set<String> VALID_SORT_PROPERTIES = Set.of(
            "id", "user", "client", "source", "product", "quantity", "unitPrice", "totalPrice",
            "paymentMethod", "transaction", "createdAt", "updatedAt", "currency", "exchangeRate",
            "comment", "totalPriceEur", "unitPriceEur"
    );

    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final ClientTypeFieldApiClient clientTypeFieldApiClient;
    private final UserService userService;
    private final SourceService sourceService;
    private final IProductService productService;
    private final IWarehouseReceiptService warehouseReceiptService;

    @Override
    @Transactional(readOnly = true)
    public void generateExcelFile(
            Sort.Direction sortDirection,
            @NonNull String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            @NonNull HttpServletResponse response,
            @NonNull List<String> selectedFields) {

        validateInputs(query, selectedFields);
        validateFilterParams(filterParams);
        validateSortProperty(sortProperty);
        
        Sort sort = createSort(sortDirection, sortProperty);
        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        
        if (clients.isEmpty()) {
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        SearchContext searchContext = prepareSearchContext(query, filterParams, clients);
        List<Purchase> purchaseList = fetchPurchases(
                query, filterParams, searchContext.clientIds(), searchContext.sourceIds(), sort);
        
        FilterIds updatedFilterIds = buildUpdatedFilterIds(purchaseList, createFilterIds());
        Map<Long, ClientDTO> clientMap = fetchClientMap(clients);
        Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap = fetchClientFieldValues(searchContext.clientIds());

        Workbook workbook = generateWorkbook(purchaseList, selectedFields, updatedFilterIds, 
                clientMap, clientFieldValuesMap);
        sendExcelFileResponse(workbook, response);
    }

    private record SearchContext(
            Long clientTypeId,
            Map<String, List<String>> clientFilterParams,
            List<Long> clientIds,
            List<Long> sourceIds
    ) {}

    private SearchContext prepareSearchContext(String query, Map<String, List<String>> filterParams, 
                                               @NonNull List<ClientDTO> clients) {
        Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, false);
        List<Long> clientIds = clients.stream()
                .filter(client -> client != null && client.getId() != null)
                .map(ClientDTO::getId)
                .toList();
        
        List<Long> sourceIds = resolveSourceIds(query, clientFilterParams, clientTypeId);
        
        return new SearchContext(clientTypeId, clientFilterParams, clientIds, sourceIds);
    }

    private List<Long> resolveSourceIds(String query, @NonNull Map<String, List<String>> clientFilterParams, Long clientTypeId) {
        if (query != null && !query.trim().isEmpty() && clientFilterParams.isEmpty() && clientTypeId == null) {
            return fetchSourceIds(query);
        }
        return null;
    }

    private FilterIds buildUpdatedFilterIds(@NonNull List<Purchase> purchaseList, @NonNull FilterIds baseFilterIds) {
        List<SourceDTO> sourceDTOs = fetchSourceDTOs(purchaseList);
        return new FilterIds(
                sourceDTOs,
                sourceDTOs.stream()
                        .filter(Objects::nonNull)
                        .map(SourceDTO::getId)
                        .filter(Objects::nonNull)
                        .toList(),
                baseFilterIds.productDTOs(),
                baseFilterIds.productIds(),
                baseFilterIds.userDTOs(),
                baseFilterIds.userIds()
        );
    }

    private record FilterIds(
            List<SourceDTO> sourceDTOs, List<Long> sourceIds,
            List<Product> productDTOs, List<Long> productIds,
            List<UserDTO> userDTOs, List<Long> userIds
    ) {
    }

    private Sort createSort(Sort.Direction sortDirection, @NonNull String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private void validateSortProperty(@NonNull String sortProperty) {
        if (sortProperty.trim().isEmpty()) {
            throw new PurchaseException("INVALID_SORT_PROPERTY", "Sort property cannot be empty");
        }
        if (!VALID_SORT_PROPERTIES.contains(sortProperty)) {
            throw new PurchaseException("INVALID_SORT_PROPERTY", 
                    String.format("Invalid sort property: %s. Valid properties: %s", 
                            sortProperty, String.join(", ", VALID_SORT_PROPERTIES)));
        }
    }

    private void validateFilterParams(Map<String, List<String>> filterParams) {
        if (filterParams == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filterParams.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new PurchaseException("INVALID_FILTER_PARAMS", 
                        "Filter parameter keys cannot be null or empty");
            }
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new PurchaseException("INVALID_FILTER_PARAMS", 
                                "Filter parameter values cannot be null or empty");
                    }
                }
            }
        }
    }

    private FilterIds createFilterIds() {
        List<Product> products = productService.getAllProducts(ALL_PRODUCTS_FILTER);
        List<Long> productIds = products.stream()
                .filter(product -> product != null && product.getId() != null)
                .map(Product::getId)
                .toList();

        List<UserDTO> userDTOs = userService.getAllUsers();
        List<Long> userIds = userDTOs.stream()
                .filter(user -> user != null && user.getId() != null)
                .map(UserDTO::getId)
                .toList();

        return new FilterIds(
                Collections.emptyList(), Collections.emptyList(),
                products, productIds,
                userDTOs, userIds);
    }

    private List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        try {
            Long clientTypeId = org.example.purchaseservice.utils.FilterUtils.extractClientTypeId(filterParams);
            Map<String, List<String>> filteredParams = org.example.purchaseservice.utils.FilterUtils.filterClientParams(filterParams, true);
            ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
            List<ClientDTO> clients = clientApiClient.searchClients(clientRequest).getBody();
            return clients != null ? clients : Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error fetching client IDs: query={}, status={}, error={}", 
                    query, e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error fetching client IDs: query={}, error={}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Map<Long, ClientDTO> fetchClientMap(@NonNull List<ClientDTO> clients) {
        return clients.stream()
                .filter(client -> client != null && client.getId() != null)
                .collect(Collectors.toMap(ClientDTO::getId, client -> client, (existing, _) -> existing));
    }

    private List<Long> fetchSourceIds(@NonNull String query) {
        if (query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<SourceDTO> sources = sourceService.findByNameContaining(query);
            return sources.stream()
                    .filter(Objects::nonNull)
                    .map(SourceDTO::getId)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Error fetching source IDs for query: {}, error={}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<SourceDTO> fetchSourceDTOs(@NonNull List<Purchase> purchases) {
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
                SourceDTO sourceDTO = sourceService.getSourceName(sourceId);
                if (sourceDTO != null) {
                    sourceDTOs.add(sourceDTO);
                }
            } catch (Exception e) {
                log.error("Failed to get source name for sourceId {}: {}", sourceId, e.getMessage(), e);
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
                                      Map<Long, ClientDTO> clientMap, 
                                      Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Data");

        List<String> sortedFields = sortFields(selectedFields);
        Map<String, String> fieldToHeader = createFieldToHeaderMap(sortedFields);
        createHeaderRow(sheet, sortedFields, fieldToHeader);
        fillDataRows(sheet, purchaseList, sortedFields, filterIds, clientMap, clientFieldValuesMap);

        return workbook;
    }
    
    private List<String> sortFields(List<String> selectedFields) {
        List<String> clientFields = new ArrayList<>();
        List<String> purchaseFields = new ArrayList<>();
        
        for (String field : selectedFields) {
            if (field.endsWith(CLIENT_SUFFIX) || field.startsWith(FIELD_PREFIX)) {
                clientFields.add(field);
            } else {
                purchaseFields.add(field);
            }
        }
        
        List<String> sorted = new ArrayList<>(clientFields);
        sorted.addAll(purchaseFields);
        return sorted;
    }

    private Map<String, String> createFieldToHeaderMap(@NonNull List<String> selectedFields) {
        Map<String, String> headerMap = new HashMap<>();
        
        headerMap.put("id-client", HEADER_ID_CLIENT);
        headerMap.put("company-client", HEADER_COMPANY_CLIENT);
        headerMap.put("createdAt-client", HEADER_CREATED_AT_CLIENT);
        headerMap.put("updatedAt-client", HEADER_UPDATED_AT_CLIENT);
        headerMap.put("source-client", HEADER_SOURCE_CLIENT);
        
        headerMap.put("id", HEADER_ID);
        headerMap.put("user", HEADER_USER);
        headerMap.put("source", HEADER_SOURCE);
        headerMap.put("product", HEADER_PRODUCT);
        headerMap.put("quantity", HEADER_QUANTITY);
        headerMap.put("unitPrice", HEADER_UNIT_PRICE);
        headerMap.put("totalPrice", HEADER_TOTAL_PRICE);
        headerMap.put("paymentMethod", HEADER_PAYMENT_METHOD);
        headerMap.put("currency", HEADER_CURRENCY);
        headerMap.put("exchangeRate", HEADER_EXCHANGE_RATE);
        headerMap.put("transaction", HEADER_TRANSACTION);
        headerMap.put("createdAt", HEADER_CREATED_AT);
        headerMap.put("updatedAt", HEADER_UPDATED_AT);
        headerMap.put("comment", HEADER_COMMENT);
        
        List<Long> fieldIds = selectedFields.stream()
                .filter(field -> field.startsWith(FIELD_PREFIX))
                .map(this::parseFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, ClientTypeFieldDTO> fieldMap = fetchClientTypeFields(fieldIds);
        
        for (String field : selectedFields) {
            if (field.startsWith(FIELD_PREFIX)) {
                Long fieldId = parseFieldId(field);
                if (fieldId != null) {
                    ClientTypeFieldDTO fieldDTO = fieldMap.get(fieldId);
                    String header = fieldDTO != null && fieldDTO.getFieldLabel() != null
                            ? fieldDTO.getFieldLabel() + HEADER_CLIENT_SUFFIX
                            : field + HEADER_CLIENT_SUFFIX;
                    headerMap.put(field, header);
                } else {
                    headerMap.put(field, field + HEADER_CLIENT_SUFFIX);
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

    private String getFieldValue(@NonNull Purchase purchase, ClientDTO client, @NonNull String field, 
                                  @NonNull FilterIds filterIds, @NonNull List<ClientFieldValueDTO> fieldValues) {
        if (field.startsWith(FIELD_PREFIX)) {
            Long fieldId = parseFieldId(field);
            return fieldId != null ? getDynamicFieldValue(fieldValues, fieldId) : "";
        }
        
        if (field.endsWith(CLIENT_SUFFIX) && client != null) {
            return getClientFieldValue(client, field);
        }
        
        return getPurchaseFieldValue(purchase, field, filterIds);
    }

    private String getClientFieldValue(@NonNull ClientDTO client, @NonNull String field) {
        return switch (field) {
            case "id-client" -> client.getId() != null ? String.valueOf(client.getId()) : "";
            case "company-client" -> client.getCompany() != null ? client.getCompany() : "";
            case "createdAt-client" -> client.getCreatedAt() != null ? client.getCreatedAt() : "";
            case "updatedAt-client" -> client.getUpdatedAt() != null ? client.getUpdatedAt() : "";
            case "source-client" -> client.getSourceId() != null ? client.getSourceId() : "";
            default -> "";
        };
    }

    private String getPurchaseFieldValue(@NonNull Purchase purchase, @NonNull String field, @NonNull FilterIds filterIds) {
        return switch (field) {
            case "id" -> purchase.getId() != null ? String.valueOf(purchase.getId()) : "";
            case "user" -> getNameFromDTOList(filterIds.userDTOs(), purchase.getUser());
            case "source" -> getNameFromDTOList(filterIds.sourceDTOs(), purchase.getSource());
            case "product" -> getProductName(purchase.getProduct(), filterIds.productDTOs());
            case "quantity" -> purchase.getQuantity() != null ? purchase.getQuantity().toString() : "";
            case "unitPrice" -> purchase.getUnitPrice() != null ? purchase.getUnitPrice().toString() : "";
            case "totalPrice" -> purchase.getTotalPrice() != null ? purchase.getTotalPrice().toString() : "";
            case "paymentMethod" -> formatPaymentMethod(purchase.getPaymentMethod());
            case "currency" -> purchase.getCurrency() != null ? purchase.getCurrency() : "";
            case "exchangeRate" -> purchase.getExchangeRate() != null ? String.valueOf(purchase.getExchangeRate()) : "";
            case "transaction" -> purchase.getTransaction() != null ? purchase.getTransaction().toString() : "";
            case "createdAt" -> purchase.getCreatedAt() != null ? purchase.getCreatedAt().toString() : "";
            case "updatedAt" -> purchase.getUpdatedAt() != null ? purchase.getUpdatedAt().toString() : "";
            case "comment" -> purchase.getComment() != null ? purchase.getComment() : "";
            default -> "";
        };
    }

    private String getProductName(Long productId, @NonNull List<Product> products) {
        if (productId == null) {
            return "";
        }
        return products.stream()
                .filter(product -> product != null && product.getId() != null && product.getId().equals(productId))
                .findFirst()
                .map(Product::getName)
                .orElse(UNKNOWN_PRODUCT);
    }

    private String formatPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "";
        }
        return paymentMethod == PaymentMethod.CASH ? "2" : "1";
    }
    
    private String getDynamicFieldValue(List<ClientFieldValueDTO> fieldValues, Long fieldId) {
        if (fieldValues == null || fieldValues.isEmpty()) {
            return "";
        }
        
        List<ClientFieldValueDTO> matchingValues = fieldValues.stream()
                .filter(fv -> fv.getFieldId() != null && fv.getFieldId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .toList();
        
        if (matchingValues.isEmpty()) {
            return "";
        }
        
        ClientFieldValueDTO firstValue = matchingValues.getFirst();
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
    
    private Map<Long, List<ClientFieldValueDTO>> fetchClientFieldValues(@NonNull List<Long> clientIds) {
        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            Map<Long, List<ClientFieldValueDTO>> result = clientApiClient.getClientFieldValuesBatch(clientIds).getBody();
            return result != null ? result : Collections.emptyMap();
        } catch (FeignException e) {
            log.error("Feign error fetching field values batch for clients: status={}, error={}", 
                    e.status(), e.getMessage(), e);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Unexpected error fetching field values batch for clients: error={}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    private <T extends IdNameDTO> String getNameFromDTOList(@NonNull List<T> dtoList, Long id) {
        if (id == null) {
            return "";
        }
        return dtoList.stream()
                .filter(dto -> dto != null && dto.getId() != null && dto.getId().equals(id))
                .findFirst()
                .map(IdNameDTO::getName)
                .orElse("");
    }

    private void validateInputs(String query, @NonNull List<String> selectedFields) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new PurchaseException("INVALID_QUERY", 
                    String.format("Search query cannot exceed %d characters", MAX_QUERY_LENGTH));
        }
        if (selectedFields.isEmpty()) {
            throw new PurchaseException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        
        validateSelectedFields(selectedFields);
    }

    private void validateSelectedFields(@NonNull List<String> selectedFields) {
        for (String field : selectedFields) {
            if (field == null || field.trim().isEmpty()) {
                throw new PurchaseException("INVALID_FIELDS", 
                        "Selected fields cannot contain null or empty values");
            }
            
            String trimmedField = field.trim();
            if (!isValidField(trimmedField)) {
                throw new PurchaseException("INVALID_FIELDS", 
                        String.format("Invalid field name: %s", trimmedField));
            }
        }
    }

    private boolean isValidField(@NonNull String field) {
        if (isDynamicField(field)) {
            return isValidDynamicField(field);
        }
        
        if (VALID_PURCHASE_FIELDS.contains(field) || VALID_CLIENT_FIELDS.contains(field)) {
            return true;
        }
        
        return isClientSuffixedField(field);
    }

    private boolean isDynamicField(@NonNull String field) {
        return field.startsWith(FIELD_PREFIX);
    }

    private boolean isValidDynamicField(@NonNull String field) {
        try {
            Long.parseLong(field.substring(FIELD_PREFIX.length()));
            return true;
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean isClientSuffixedField(@NonNull String field) {
        if (!field.endsWith(CLIENT_SUFFIX)) {
            return false;
        }
        String baseField = field.substring(0, field.length() - CLIENT_SUFFIX.length());
        return VALID_PURCHASE_FIELDS.contains(baseField);
    }

    private void sendExcelFileResponse(@NonNull Workbook workbook, @NonNull HttpServletResponse response) {
        try {
            response.setContentType(EXCEL_CONTENT_TYPE);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
            String filename = EXCEL_FILENAME_PREFIX + dateStr + EXCEL_FILENAME_SUFFIX;
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            try {
                workbook.close();
            } catch (IOException closeException) {
                log.error("Failed to close workbook: {}", closeException.getMessage(), closeException);
            }
            throw new PurchaseException("EXCEL_GENERATION_ERROR", 
                String.format("Error generating Excel file: %s", e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseReportDTO generateReport(String query, @NonNull Map<String, List<String>> filterParams) {
        validateFilterParams(filterParams);
        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        SearchContext searchContext = prepareSearchContext(query, filterParams, clients);
        
        List<Purchase> purchaseList = fetchPurchases(query, filterParams, clients, searchContext.sourceIds());
        FilterIds updatedFilterIds = buildUpdatedFilterIds(purchaseList, createFilterIds());
        List<WarehouseReceipt> warehouseReceipts = fetchWarehouseReceipts(filterParams);

        return buildReportFromData(purchaseList, warehouseReceipts, updatedFilterIds);
    }

    private PurchaseReportDTO buildReportFromData(@NonNull List<Purchase> purchaseList,
                                                  @NonNull List<WarehouseReceipt> warehouseReceipts,
                                                  @NonNull FilterIds filterIds) {
        Map<Long, Double> totalCollectedByProduct = calculateTotalCollectedByProduct(purchaseList);
        Map<Long, Double> totalDeliveredByProduct = calculateTotalDeliveredByProduct(warehouseReceipts);
        Map<String, Map<Long, Double>> byDrivers = calculateByDrivers(purchaseList, filterIds);
        Map<String, Map<Long, Double>> byAttractors = calculateByAttractors(purchaseList, filterIds);
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
                                                ? p.getTotalPrice().divide(p.getQuantity(), PRICE_SCALE, PRICE_ROUNDING_MODE).doubleValue()
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
    public void generateComparisonExcelFile(@NonNull String purchaseDataFrom, @NonNull String purchaseDataTo, 
                                            @NonNull HttpServletResponse response) {
        try {
            DateRange dateRange = parseAndValidateDateRange(purchaseDataFrom, purchaseDataTo);
            List<ClientDTO> clients = fetchAllClients();
            Map<Long, String> productNames = fetchProductNames();
            
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Purchase Report");
            
            createComparisonHeaderRow(sheet);
            
            Map<Long, Map<Long, BigDecimal>> clientProductSums = calculateClientProductSums(clients, dateRange);
            fillComparisonDataRows(sheet, clients, clientProductSums, productNames);
            
            autoSizeColumns(sheet);
            sendComparisonExcelResponse(workbook, response);

        } catch (DateTimeParseException e) {
            log.error("Invalid date format for purchaseDataFrom or purchaseDataTo. Expected format: {}", DATE_PATTERN, e);
            throw new PurchaseException("INVALID_DATE_FORMAT", 
                    String.format("Invalid date format. Please use %s", DATE_PATTERN));
        } catch (PurchaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            throw new PurchaseException("EXCEL_GENERATION_ERROR", 
                String.format("Failed to generate Excel file: %s", e.getMessage()));
        }
    }

    private record DateRange(LocalDateTime fromDateTime, LocalDateTime toDateTime) {}

    private DateRange parseAndValidateDateRange(@NonNull String purchaseDataFrom, @NonNull String purchaseDataTo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        LocalDate fromDate = LocalDate.parse(purchaseDataFrom, formatter);
        LocalDate toDate = LocalDate.parse(purchaseDataTo, formatter);

        if (fromDate.isAfter(toDate)) {
            throw new PurchaseException("INVALID_DATE_RANGE", 
                    "Start date cannot be after end date");
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59, 999999999);
        return new DateRange(fromDateTime, toDateTime);
    }

    private Map<Long, String> fetchProductNames() {
        return productService.getAllProducts(ALL_PRODUCTS_FILTER).stream()
                .filter(product -> product != null && product.getId() != null && product.getName() != null)
                .collect(Collectors.toMap(Product::getId, Product::getName, (existing, _) -> existing));
    }

    private void createComparisonHeaderRow(@NonNull XSSFSheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Client ID", "Company", "Product", "Total Volume"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
    }

    private Map<Long, Map<Long, BigDecimal>> calculateClientProductSums(@NonNull List<ClientDTO> clients, 
                                                                        @NonNull DateRange dateRange) {
        List<Long> clientIds = clients.stream()
                .filter(client -> client != null && client.getId() != null)
                .map(ClientDTO::getId)
                .toList();

        Specification<Purchase> spec = buildPurchaseSpecification(clientIds, dateRange);
        List<Purchase> allPurchases = purchaseRepository.findAll(spec, Pageable.unpaged()).getContent();

        return allPurchases.stream()
                .filter(p -> p.getClient() != null && p.getProduct() != null && p.getQuantity() != null)
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
    }

    private Specification<Purchase> buildPurchaseSpecification(@NonNull List<Long> clientIds, @NonNull DateRange dateRange) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            }
            predicates.add(cb.between(root.get("createdAt"), dateRange.fromDateTime(), dateRange.toDateTime()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void fillComparisonDataRows(@NonNull XSSFSheet sheet, @NonNull List<ClientDTO> clients,
                                       @NonNull Map<Long, Map<Long, BigDecimal>> clientProductSums,
                                       @NonNull Map<Long, String> productNames) {
        int rowNum = 1;
        for (ClientDTO client : clients) {
            if (client == null || client.getId() == null) {
                continue;
            }
            
            Map<Long, BigDecimal> productSums = clientProductSums.getOrDefault(client.getId(), Collections.emptyMap());
            
            if (productSums.isEmpty()) {
                createComparisonRow(sheet, rowNum++, client, DEFAULT_PRODUCT_ID, BigDecimal.ZERO, productNames);
            } else {
                for (Map.Entry<Long, BigDecimal> entry : productSums.entrySet()) {
                    createComparisonRow(sheet, rowNum++, client, entry.getKey(), entry.getValue(), productNames);
                }
            }
        }
    }

    private void createComparisonRow(@NonNull XSSFSheet sheet, int rowNum, @NonNull ClientDTO client,
                                    Long productId, @NonNull BigDecimal quantity, @NonNull Map<Long, String> productNames) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(client.getId());
        row.createCell(1).setCellValue(client.getCompany() != null ? client.getCompany() : "");
        row.createCell(2).setCellValue(productNames.getOrDefault(productId, UNKNOWN_PRODUCT));
        row.createCell(3).setCellValue(quantity.doubleValue());
    }

    private void autoSizeColumns(@NonNull XSSFSheet sheet) {
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void sendComparisonExcelResponse(@NonNull XSSFWorkbook workbook, @NonNull HttpServletResponse response) throws IOException {
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=" + COMPARISON_EXCEL_FILENAME);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private Long parseFieldId(String field) {
        try {
            return Long.parseLong(field.substring(FIELD_PREFIX.length()));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.error("Invalid field ID in field name {}: {}", field, e.getMessage());
            return null;
        }
    }

    private Map<Long, ClientTypeFieldDTO> fetchClientTypeFields(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<ClientTypeFieldDTO> fields = clientTypeFieldApiClient.getFieldsByIds(fieldIds).getBody();
            if (fields != null) {
                return fields.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(ClientTypeFieldDTO::getId, field -> field, (existing, _) -> existing));
            }
        } catch (FeignException e) {
            log.error("Feign error fetching client type fields: status={}, error={}", 
                    e.status(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching client type fields: error={}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    private List<ClientDTO> fetchAllClients() {
        try {
            ClientSearchRequest request = new ClientSearchRequest(null, null, null);
            List<ClientDTO> clients = clientApiClient.searchClients(request).getBody();
            return clients != null ? clients : Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error fetching all clients: status={}, error={}", 
                    e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error fetching all clients: error={}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}