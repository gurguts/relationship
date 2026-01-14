package org.example.purchaseservice.services.purchase;

import feign.FeignException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.clients.*;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.models.dto.impl.IdNameDTO;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.*;
import org.example.purchaseservice.spec.PurchaseSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSpecialOperationsService implements IPurchaseSpecialOperationsService {

    private static final int MAX_QUERY_LENGTH = 255;
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String FIELD_PREFIX = "field_";
    private static final String CLIENT_SUFFIX = "-client";
    private static final String UNKNOWN_PRODUCT = "Unknown Product";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String EXCEL_FILENAME_PREFIX = "purchase_data_";
    private static final String EXCEL_FILENAME_SUFFIX = ".xlsx";
    private static final String COMPARISON_EXCEL_FILENAME = "purchase_report.xlsx";
    private static final Long DEFAULT_PRODUCT_ID = 1L;

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
    private static final String HEADER_TOTAL_PRICE_EUR = "Всього сплачено EUR";
    private static final String HEADER_PAYMENT_METHOD = "Метод оплати";
    private static final String HEADER_CURRENCY = "Валюта";
    private static final String HEADER_EXCHANGE_RATE = "Курс";
    private static final String HEADER_TRANSACTION = "Id транзакції";
    private static final String HEADER_CREATED_AT = "Дата створення";
    private static final String HEADER_UPDATED_AT = "Дата оновлення";
    private static final String HEADER_COMMENT = "Коментар";
    private static final String HEADER_CLIENT_SUFFIX = " (клієнта)";
    
    private static final Set<String> VALID_PURCHASE_FIELDS = Set.of(
            "id", "user", "source", "product", "quantity", "unitPrice", "totalPrice", "totalPriceEur",
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
    private final IUserService userService;
    private final ISourceService sourceService;
    private final IProductService productService;
    @Getter
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
        List<SourceDTO> clientSourceDTOs = fetchClientSourceDTOs(clients);
        FilterIds filterIdsWithClientSources = new FilterIds(
                mergeSourceDTOs(updatedFilterIds.sourceDTOs(), clientSourceDTOs),
                updatedFilterIds.sourceIds(),
                updatedFilterIds.productDTOs(),
                updatedFilterIds.productIds(),
                updatedFilterIds.userDTOs(),
                updatedFilterIds.userIds()
        );

        Workbook workbook = generateWorkbook(purchaseList, selectedFields, filterIdsWithClientSources, 
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
            } catch (Exception _) {
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
        headerMap.put("totalPriceEur", HEADER_TOTAL_PRICE_EUR);
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
                    String header;
                    if (fieldDTO != null) {
                        String label = fieldDTO.getFieldLabel();
                        if (label != null && !label.trim().isEmpty()) {
                            header = label + HEADER_CLIENT_SUFFIX;
                        } else {
                            String name = fieldDTO.getFieldName();
                            header = (name != null && !name.trim().isEmpty()) 
                                    ? name + HEADER_CLIENT_SUFFIX 
                                    : field + HEADER_CLIENT_SUFFIX;
                        }
                    } else {
                        header = field + HEADER_CLIENT_SUFFIX;
                    }
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
            return getClientFieldValue(client, field, filterIds);
        }
        
        return getPurchaseFieldValue(purchase, field, filterIds);
    }

    private String getClientFieldValue(@NonNull ClientDTO client, @NonNull String field, @NonNull FilterIds filterIds) {
        return switch (field) {
            case "id-client" -> client.getId() != null ? String.valueOf(client.getId()) : "";
            case "company-client" -> client.getCompany() != null ? client.getCompany() : "";
            case "createdAt-client" -> client.getCreatedAt() != null ? client.getCreatedAt() : "";
            case "updatedAt-client" -> client.getUpdatedAt() != null ? client.getUpdatedAt() : "";
            case "source-client" -> getClientSourceName(client, filterIds.sourceDTOs());
            default -> "";
        };
    }

    private String getClientSourceName(@NonNull ClientDTO client, @NonNull List<SourceDTO> sourceDTOs) {
        try {
            java.lang.reflect.Method getSourceMethod = client.getClass().getMethod("getSource");
            Object sourceObj = getSourceMethod.invoke(client);
            if (sourceObj != null) {
                java.lang.reflect.Method getNameMethod = sourceObj.getClass().getMethod("getName");
                Object sourceName = getNameMethod.invoke(sourceObj);
                if (sourceName != null) {
                    return sourceName.toString();
                }
            }
        } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException _) {
        }
        
        String sourceId = client.getSourceId();
        if (sourceId == null || sourceId.trim().isEmpty()) {
            return "";
        }
        try {
            Long sourceIdLong = Long.parseLong(sourceId.trim());
            return getNameFromDTOList(sourceDTOs, sourceIdLong);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private List<SourceDTO> fetchClientSourceDTOs(@NonNull List<ClientDTO> clients) {
        Map<Long, SourceDTO> uniqueSources = new HashMap<>();
        
        for (ClientDTO client : clients) {
            try {
                java.lang.reflect.Method getSourceMethod = client.getClass().getMethod("getSource");
                Object sourceObj = getSourceMethod.invoke(client);
                if (sourceObj != null) {
                    SourceDTO sourceDTO = (SourceDTO) sourceObj;
                    if (sourceDTO.getId() != null) {
                        uniqueSources.put(sourceDTO.getId(), sourceDTO);
                    }
                }
            } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException | ClassCastException _) {
            }
            
            String sourceId = client.getSourceId();
            if (sourceId != null && !sourceId.trim().isEmpty()) {
                try {
                    Long sourceIdLong = Long.parseLong(sourceId.trim());
                    if (!uniqueSources.containsKey(sourceIdLong)) {
                        SourceDTO sourceDTO = sourceService.getSourceName(sourceIdLong);
                        if (sourceDTO != null) {
                            uniqueSources.put(sourceIdLong, sourceDTO);
                        }
                    }
                } catch (Exception _) {
                }
            }
        }

        return new ArrayList<>(uniqueSources.values());
    }

    private List<SourceDTO> mergeSourceDTOs(@NonNull List<SourceDTO> purchaseSources, @NonNull List<SourceDTO> clientSources) {
        Map<Long, SourceDTO> mergedMap = new HashMap<>();
        purchaseSources.forEach(source -> {
            if (source != null && source.getId() != null) {
                mergedMap.put(source.getId(), source);
            }
        });
        clientSources.forEach(source -> {
            if (source != null && source.getId() != null) {
                mergedMap.putIfAbsent(source.getId(), source);
            }
        });
        return new ArrayList<>(mergedMap.values());
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
            case "totalPriceEur" -> purchase.getTotalPriceEur() != null ? purchase.getTotalPriceEur().toString() : "";
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
        
        Map<Long, List<ClientFieldValueDTO>> result = new HashMap<>();
        int batchSize = 100;
        
        for (int i = 0; i < clientIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, clientIds.size());
            List<Long> batch = clientIds.subList(i, endIndex);
            
            try {
                org.example.purchaseservice.models.dto.client.ClientIdsRequest request = 
                        new org.example.purchaseservice.models.dto.client.ClientIdsRequest(batch);
                Map<Long, List<ClientFieldValueDTO>> batchResult = clientApiClient.getClientFieldValuesBatch(request).getBody();
                if (batchResult != null) {
                    result.putAll(batchResult);
                }
            } catch (FeignException e) {
                log.error("Feign error fetching field values batch for clients: status={}, error={}", 
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error fetching field values batch for clients: error={}", e.getMessage(), e);
            }
        }
        
        return result;
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
            return null;
        }
    }

    private Map<Long, ClientTypeFieldDTO> fetchClientTypeFields(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, ClientTypeFieldDTO> result = new HashMap<>();
        int batchSize = 100;
        
        for (int i = 0; i < fieldIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, fieldIds.size());
            List<Long> batch = fieldIds.subList(i, endIndex);
            
            try {
                org.example.purchaseservice.models.dto.clienttype.FieldIdsRequest request = 
                        new org.example.purchaseservice.models.dto.clienttype.FieldIdsRequest(batch);
                List<ClientTypeFieldDTO> fields = clientTypeFieldApiClient.getFieldsByIds(request).getBody();
                if (fields != null) {
                    fields.stream()
                            .filter(Objects::nonNull)
                            .forEach(field -> result.putIfAbsent(field.getId(), field));
                }
            } catch (FeignException e) {
                log.error("Feign error fetching client type fields: status={}, error={}", 
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error fetching client type fields: error={}", e.getMessage(), e);
            }
        }
        
        return result;
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