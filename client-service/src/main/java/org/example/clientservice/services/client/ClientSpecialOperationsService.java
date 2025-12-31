package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.ClientFilterIds;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.client.ClientExportResult;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.IClientSpecialOperationsService;
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.services.clienttype.ClientTypeFieldService;
import org.example.clientservice.services.clienttype.ClientTypeService;
import org.example.clientservice.spec.ClientSpecification;
import org.example.clientservice.utils.FilenameUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSpecialOperationsService implements IClientSpecialOperationsService {
    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> VALID_STATIC_FIELDS = Set.of(
            "id", "company", "createdAt", "updatedAt", "source");
    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String DEFAULT_CLIENT_TYPE_NAME = "клієнти";
    private static final String FILENAME_TEMPLATE = "клієнти_%s_%s.xlsx";
    private static final int MAX_QUERY_LENGTH = 255;
    private static final String FIELD_PREFIX = "field_";
    private static final int FIELD_PREFIX_LENGTH = 6;
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final String BOOLEAN_TRUE_UA = "Так";
    private static final String BOOLEAN_FALSE_UA = "Ні";
    private static final String EMPTY_STRING = "";

    private final ISourceService sourceService;
    private final ClientTypeFieldService clientTypeFieldService;
    private final ClientTypeService clientTypeService;

    @Override
    @Transactional(readOnly = true)
    @NonNull
    public ClientExportResult exportClientsToExcel(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            @NonNull List<String> selectedFields
    ) {
        log.info("Starting export to Excel with {} fields", selectedFields.size());
        
        validateInputs(query, selectedFields);

        Sort sort = createSort(sortDirection, sortProperty);
        ClientFilterIds filterIds = buildFilterIds(query);

        List<Client> clientList = fetchClients(query, filterParams, filterIds, sort);

        Workbook workbook = generateWorkbook(clientList, selectedFields, filterIds);

        byte[] excelData = convertWorkbookToBytes(workbook);
        String filename = generateFilename(filterParams);

        log.info("Successfully exported {} clients to Excel", clientList.size());
        return new ClientExportResult(excelData, filename);
    }

    private void validateInputs(String query, @NonNull List<String> selectedFields) {
        validateQuery(query);
        validateSelectedFields(selectedFields);
    }

    private void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ClientException("INVALID_QUERY", 
                    String.format("Search query cannot exceed %d characters", MAX_QUERY_LENGTH));
        }
    }

    private void validateSelectedFields(@NonNull List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        
        List<Long> fieldIds = extractFieldIds(selectedFields);
        Map<Long, ClientTypeField> fieldMap = loadFieldMap(fieldIds);
        
        validateFieldNames(selectedFields, fieldMap);
    }

    private List<Long> extractFieldIds(@NonNull List<String> selectedFields) {
        return selectedFields.stream()
                .filter(field -> field.startsWith(FIELD_PREFIX))
                .map(this::parseFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long parseFieldId(@NonNull String field) {
        try {
            return Long.parseLong(field.substring(FIELD_PREFIX_LENGTH));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private Map<Long, ClientTypeField> loadFieldMap(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        FieldIdsRequest request = new FieldIdsRequest(fieldIds);
        return clientTypeFieldService.getFieldsByIds(request).stream()
                .collect(Collectors.toMap(ClientTypeField::getId, field -> field));
    }

    private void validateFieldNames(@NonNull List<String> selectedFields, @NonNull Map<Long, ClientTypeField> fieldMap) {
        for (String field : selectedFields) {
            if (!VALID_STATIC_FIELDS.contains(field) && !field.startsWith(FIELD_PREFIX)) {
                throw new ClientException("INVALID_FIELDS", 
                        String.format("Invalid field specified for export: %s", field));
            }
            if (field.startsWith(FIELD_PREFIX)) {
                validateDynamicField(field, fieldMap);
            }
        }
    }

    private void validateDynamicField(@NonNull String field, @NonNull Map<Long, ClientTypeField> fieldMap) {
        Long fieldId = parseFieldId(field);
        if (fieldId == null) {
            throw new ClientException("INVALID_FIELDS", 
                    String.format("Invalid field ID format: %s", field));
        }
        if (!fieldMap.containsKey(fieldId)) {
            throw new ClientException("INVALID_FIELDS", 
                    String.format("Dynamic field not found: %s", field));
        }
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private ClientFilterIds buildFilterIds(String query) {
        String normalizedQuery = normalizeQuery(query);
        
        if (normalizedQuery != null) {
            return fetchFilterIds(normalizedQuery);
        }
        
        return new ClientFilterIds(Collections.emptyList(), Collections.emptyList());
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private ClientFilterIds fetchFilterIds(@NonNull String query) {
        List<Source> sourceDTOs = sourceService.findByNameContaining(query);
        List<Long> sourceIds = sourceDTOs.stream()
                .map(Source::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ClientFilterIds(sourceDTOs, sourceIds);
    }

    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, 
                                     ClientFilterIds filterIds, @NonNull Sort sort) {
        String normalizedQuery = normalizeQuery(query);
        Long clientTypeId = extractClientTypeId(filterParams);
        List<Long> sourceIdsForSpec = normalizedQuery != null ? filterIds.sourceIds() : null;
        
        Specification<Client> spec = createClientSpecification(normalizedQuery, filterParams, 
                sourceIdsForSpec, clientTypeId);
        
        return executeClientQuery(spec, sort);
    }

    private Long extractClientTypeId(Map<String, List<String>> filterParams) {
        if (filterParams == null || !filterParams.containsKey(FILTER_KEY_CLIENT_TYPE_ID)) {
            return null;
        }
        
        List<String> clientTypeIdList = filterParams.get(FILTER_KEY_CLIENT_TYPE_ID);
        if (clientTypeIdList == null || clientTypeIdList.isEmpty()) {
            return null;
        }
        
        try {
            return Long.parseLong(clientTypeIdList.getFirst());
        } catch (NumberFormatException e) {
            log.warn("Invalid clientTypeId format: {}", clientTypeIdList.getFirst());
            return null;
        }
    }

    private Specification<Client> createClientSpecification(String query, Map<String, List<String>> filterParams,
                                                           List<Long> sourceIds, Long clientTypeId) {
        return new ClientSpecification(query, filterParams, sourceIds, clientTypeId);
    }

    private List<Client> executeClientQuery(@NonNull Specification<Client> spec, @NonNull Sort sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Client> cq = cb.createQuery(Client.class);
        Root<Client> root = cq.from(Client.class);

        configureFetches(root);
        cq.distinct(true);

        Predicate specPredicate = spec.toPredicate(root, cq, cb);
        if (specPredicate != null) {
            cq.where(specPredicate);
        }

        applySorting(cq, root, cb, sort);
        
        TypedQuery<Client> typedQuery = entityManager.createQuery(cq);
        return typedQuery.getResultList();
    }

    private void configureFetches(@NonNull Root<Client> root) {
        root.fetch("clientType", JoinType.LEFT);

        Fetch<Object, Object> fieldValuesFetch = root.fetch("fieldValues", JoinType.LEFT);
        fieldValuesFetch.fetch("field", JoinType.LEFT);
        fieldValuesFetch.fetch("valueList", JoinType.LEFT);
    }

    private void applySorting(@NonNull CriteriaQuery<Client> cq, @NonNull Root<Client> root,
                             @NonNull CriteriaBuilder cb, @NonNull Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            Path<?> path = root.get(order.getProperty());
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        cq.orderBy(orders);
    }

    private Workbook generateWorkbook(@NonNull List<Client> clientList, @NonNull List<String> selectedFields, 
                                     @NonNull ClientFilterIds filterIds) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Client Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap(selectedFields);
        Map<Long, Source> sourceMap = buildSourceMap(clientList);
        
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, clientList, selectedFields, sourceMap);

        return workbook;
    }

    private Map<String, String> createFieldToHeaderMap(@NonNull List<String> selectedFields) {
        Map<String, String> headerMap = createStaticHeaderMap();

        List<Long> fieldIds = extractFieldIds(selectedFields);
        if (!fieldIds.isEmpty()) {
            Map<Long, ClientTypeField> fieldMap = loadFieldMap(fieldIds);
            addDynamicFieldHeaders(headerMap, selectedFields, fieldMap);
        }
        
        return headerMap;
    }

    private Map<String, String> createStaticHeaderMap() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("id", "Id");
        headerMap.put("company", "Компанія");
        headerMap.put("createdAt", "Дата створення");
        headerMap.put("updatedAt", "Дата оновлення");
        headerMap.put("source", "Залучення");
        return headerMap;
    }

    private void addDynamicFieldHeaders(@NonNull Map<String, String> headerMap, 
                                       @NonNull List<String> selectedFields,
                                       @NonNull Map<Long, ClientTypeField> fieldMap) {
        for (String field : selectedFields) {
            if (field.startsWith(FIELD_PREFIX)) {
                Long fieldId = parseFieldId(field);
                if (fieldId != null) {
                    ClientTypeField clientTypeField = fieldMap.get(fieldId);
                    if (clientTypeField != null) {
                        headerMap.put(field, clientTypeField.getFieldLabel());
                    } else {
                        log.warn("Field not found for field ID: {}", fieldId);
                        headerMap.put(field, field);
                    }
                } else {
                    log.warn("Invalid field ID in field name: {}", field);
                    headerMap.put(field, field);
                }
            }
        }
    }

    private Map<Long, Source> buildSourceMap(@NonNull List<Client> clientList) {
        Set<Long> sourceIds = clientList.stream()
                .map(Client::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        if (sourceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<Source> allSources = sourceService.getAllSources();
        return allSources.stream()
                .filter(source -> source.getId() != null && sourceIds.contains(source.getId()))
                .collect(Collectors.toMap(Source::getId, source -> source));
    }

    private void createHeaderRow(@NonNull Sheet sheet, @NonNull List<String> selectedFields, 
                                 @NonNull Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            String header = fieldToHeader.getOrDefault(field, field);
            headerRow.createCell(colIndex++).setCellValue(header);
        }
    }

    private void fillDataRows(@NonNull Sheet sheet, @NonNull List<Client> clientList, 
                             @NonNull List<String> selectedFields, @NonNull Map<Long, Source> sourceMap) {
        int rowIndex = 1;
        for (Client client : clientList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(client, field, sourceMap));
            }
        }
    }

    private String getFieldValue(@NonNull Client client, @NonNull String field, 
                                @NonNull Map<Long, Source> sourceMap) {
        if (field.startsWith(FIELD_PREFIX)) {
            return getDynamicFieldValue(client, field);
        }

        return switch (field) {
            case "id" -> client.getId() != null ? String.valueOf(client.getId()) : EMPTY_STRING;
            case "company" -> client.getCompany();
            case "createdAt" -> client.getCreatedAt() != null ? client.getCreatedAt().toString() : EMPTY_STRING;
            case "updatedAt" -> client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : EMPTY_STRING;
            case "source" -> getSourceName(client, sourceMap);
            default -> EMPTY_STRING;
        };
    }

    private String getSourceName(@NonNull Client client, @NonNull Map<Long, Source> sourceMap) {
        if (client.getSourceId() == null) {
            return EMPTY_STRING;
        }
        
        Source source = sourceMap.get(client.getSourceId());
        return source != null ? source.getName() : EMPTY_STRING;
    }

    private String getDynamicFieldValue(@NonNull Client client, @NonNull String field) {
        try {
            Long fieldId = parseFieldId(field);
            if (fieldId == null) {
                return EMPTY_STRING;
            }
            return getDynamicFieldValueById(client, fieldId);
        } catch (Exception e) {
            log.warn("Failed to get dynamic field value for field {}: {}", field, e.getMessage());
            return EMPTY_STRING;
        }
    }

    private String getDynamicFieldValueById(@NonNull Client client, @NonNull Long fieldId) {
        if (client.getFieldValues() == null || client.getFieldValues().isEmpty()) {
            return EMPTY_STRING;
        }
        
        List<ClientFieldValue> fieldValues = findFieldValuesByFieldId(client, fieldId);
        
        if (fieldValues.isEmpty()) {
            return EMPTY_STRING;
        }
        
        ClientTypeField field = extractFieldType(fieldValues);

        return formatFieldValues(fieldValues, field);
    }

    private List<ClientFieldValue> findFieldValuesByFieldId(@NonNull Client client, @NonNull Long fieldId) {
        return client.getFieldValues().stream()
                .filter(fv -> fv.getField().getId() != null && fv.getField().getId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .collect(Collectors.toList());
    }

    private ClientTypeField extractFieldType(@NonNull List<ClientFieldValue> fieldValues) {
        ClientFieldValue firstValue = fieldValues.getFirst();
        return firstValue.getField();
    }

    private String formatFieldValues(@NonNull List<ClientFieldValue> fieldValues, @NonNull ClientTypeField field) {
        boolean allowMultiple = Boolean.TRUE.equals(field.getAllowMultiple());
        
        if (allowMultiple && fieldValues.size() > 1) {
            return formatMultipleFieldValues(fieldValues, field);
        } else {
            return formatFieldValue(fieldValues.getFirst(), field);
        }
    }

    private String formatMultipleFieldValues(@NonNull List<ClientFieldValue> fieldValues, @NonNull ClientTypeField field) {
        return fieldValues.stream()
                .map(fv -> formatFieldValue(fv, field))
                .filter(v -> !v.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String formatFieldValue(@NonNull ClientFieldValue fieldValue, @NonNull ClientTypeField field) {
        return switch (field.getFieldType()) {
            case TEXT, PHONE -> fieldValue.getValueText() != null ? fieldValue.getValueText() : EMPTY_STRING;
            case NUMBER -> fieldValue.getValueNumber() != null ? String.valueOf(fieldValue.getValueNumber()) : EMPTY_STRING;
            case DATE -> fieldValue.getValueDate() != null ? fieldValue.getValueDate().toString() : EMPTY_STRING;
            case BOOLEAN -> formatBooleanValue(fieldValue.getValueBoolean());
            case LIST -> formatListValue(fieldValue);
        };
    }

    private String formatBooleanValue(Boolean value) {
        if (value == null) {
            return EMPTY_STRING;
        }
        return value ? BOOLEAN_TRUE_UA : BOOLEAN_FALSE_UA;
    }

    private String formatListValue(@NonNull ClientFieldValue fieldValue) {
        if (fieldValue.getValueList() != null && fieldValue.getValueList().getValue() != null) {
            return fieldValue.getValueList().getValue();
        }
        return EMPTY_STRING;
    }

    private byte[] convertWorkbookToBytes(@NonNull Workbook workbook) {
        byte[] result = new byte[0];
        try (workbook; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try {
                workbook.write(baos);
                result = baos.toByteArray();
            } catch (IOException e) {
                log.error("Error generating Excel file: {}", e.getMessage(), e);
                throw new ClientException("EXCEL_GENERATION_ERROR",
                        String.format("Error generating Excel file: %s", e.getMessage()));
            }
        } catch (IOException e) {
            log.warn("Failed to close workbook: {}", e.getMessage());
        }
        return result;
    }

    private String generateFilename(Map<String, List<String>> filterParams) {
        String dateTime = LocalDateTime.now().format(FILENAME_DATE_FORMATTER);
        String clientTypeName = extractClientTypeName(filterParams);
        return String.format(FILENAME_TEMPLATE, clientTypeName, dateTime);
    }

    private String extractClientTypeName(Map<String, List<String>> filterParams) {
        Long clientTypeId = extractClientTypeId(filterParams);
        
        if (clientTypeId == null) {
            return DEFAULT_CLIENT_TYPE_NAME;
        }
        
        try {
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
            return FilenameUtils.sanitizeFilename(clientType.getName());
        } catch (ClientException e) {
            log.warn("Failed to get client type name for filename generation: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error getting client type name: {}", e.getMessage(), e);
        }
        
        return DEFAULT_CLIENT_TYPE_NAME;
    }
}
