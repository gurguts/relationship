package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.services.clienttype.ClientTypeFieldService;
import org.example.clientservice.spec.ClientSpecification;
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
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSpecialOperationsService implements IClientSpecialOperationsService {
    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> VALID_STATIC_FIELDS = Set.of(
            "id", "company", "createdAt", "updatedAt", "source");

    private final ISourceService sourceService;
    private final ClientTypeFieldService clientTypeFieldService;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            List<String> selectedFields
    ) {
        validateInputs(query, filterParams, selectedFields);

        Sort sort = createSort(sortDirection, sortProperty);
        FilterIds filterIds;
        if (query != null && !query.trim().isEmpty()) {
            filterIds = fetchFilterIds(query);
        } else {
            List<Source> allSources = sourceService.getAllSources();
            filterIds = new FilterIds(allSources, List.of());
        }

        List<Client> clientList = fetchClients(query, filterParams, filterIds, sort);

        Workbook workbook = generateWorkbook(clientList, selectedFields, filterIds);

        return convertWorkbookToBytes(workbook);
    }

    private void validateInputs(String query, Map<String, List<String>> filterParams, List<String> selectedFields) {
        if (query != null && query.length() > 255) {
            throw new ClientException("INVALID_QUERY", "Search query cannot exceed 255 characters");
        }
        if (filterParams != null) {
            // Валидация ключей фильтров:
            // - Стандартные ключи (createdAtFrom, createdAtTo, updatedAtFrom, updatedAtTo, source, showInactive, clientTypeId)
            // - Диапазоны (ключи, заканчивающиеся на From/To)
            // - Динамические поля (обрабатываются в ClientSpecification)
            // Все остальные ключи будут проигнорированы в ClientSpecification
        }
        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        
        List<Long> fieldIds = selectedFields.stream()
                .filter(field -> field.startsWith("field_"))
                .map(field -> {
                    try {
                        return Long.parseLong(field.substring(6));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, ClientTypeField> fieldMap = new HashMap<>();
        if (!fieldIds.isEmpty()) {
            fieldMap = clientTypeFieldService.getFieldsByIds(fieldIds).stream()
                    .collect(Collectors.toMap(ClientTypeField::getId, field -> field));
        }
        
        for (String field : selectedFields) {
            if (!VALID_STATIC_FIELDS.contains(field) && !field.startsWith("field_")) {
                throw new ClientException("INVALID_FIELDS", String.format("Invalid field specified for export: %s", field));
            }
            if (field.startsWith("field_")) {
                try {
                    Long fieldId = Long.parseLong(field.substring(6));
                    if (!fieldMap.containsKey(fieldId)) {
                        throw new ClientException("INVALID_FIELDS", String.format("Dynamic field not found: %s", field));
                    }
                } catch (NumberFormatException e) {
                    throw new ClientException("INVALID_FIELDS", String.format("Invalid field ID format: %s", field));
                }
            }
        }
    }

    private record FilterIds(
            List<Source> sourceDTOs, List<Long> sourceIds
    ) {
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private FilterIds fetchFilterIds(String query) {
        List<Source> sourceDTOs = sourceService.findByNameContaining(query);
        List<Long> sourceIds = sourceDTOs.stream().map(Source::getId).toList();

        return new FilterIds(sourceDTOs, sourceIds);
    }


    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      Sort sort) {
        String normalizedQuery = null;
        if (query != null) {
            String trimmed = query.trim();
            if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed)) {
                normalizedQuery = query;
            }
        }
        
        Long clientTypeId = null;
        if (filterParams != null && filterParams.containsKey("clientTypeId")) {
            List<String> clientTypeIdList = filterParams.get("clientTypeId");
            if (clientTypeIdList != null && !clientTypeIdList.isEmpty()) {
                try {
                    clientTypeId = Long.parseLong(clientTypeIdList.get(0));
                } catch (NumberFormatException e) {
                }
            }
        }

        List<Long> sourceIdsForSpec = (normalizedQuery != null && !normalizedQuery.trim().isEmpty()) 
            ? filterIds.sourceIds() 
            : null;
        
        Specification<Client> spec = new ClientSpecification(
                normalizedQuery,
                filterParams,
                sourceIdsForSpec,
                clientTypeId
        );

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Client> cq = cb.createQuery(Client.class);
        Root<Client> root = cq.from(Client.class);

        root.fetch("clientType", JoinType.LEFT);

        Fetch<Object, Object> fieldValuesFetch = root.fetch("fieldValues", JoinType.LEFT);
        fieldValuesFetch.fetch("field", JoinType.LEFT);
        fieldValuesFetch.fetch("valueList", JoinType.LEFT);

        cq.distinct(true);

        Predicate specPredicate = spec.toPredicate(root, cq, cb);
        if (specPredicate != null) {
            cq.where(specPredicate);
        }

        if (sort != null) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order order : sort) {
                Path<?> path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            cq.orderBy(orders);
        }
        
        TypedQuery<Client> typedQuery = entityManager.createQuery(cq);
        return typedQuery.getResultList();
    }
    

    private Workbook generateWorkbook(List<Client> clientList, List<String> selectedFields, FilterIds filterIds) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Client Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap(selectedFields);
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, clientList, selectedFields, filterIds);

        return workbook;
    }

    private Map<String, String> createFieldToHeaderMap(List<String> selectedFields) {
        Map<String, String> headerMap = new HashMap<>();

        headerMap.put("id", "Id");
        headerMap.put("company", "Компанія");
        headerMap.put("createdAt", "Дата створення");
        headerMap.put("updatedAt", "Дата оновлення");
        headerMap.put("source", "Залучення");

        List<Long> fieldIds = selectedFields.stream()
                .filter(field -> field.startsWith("field_"))
                .map(field -> {
                    try {
                        return Long.parseLong(field.substring(6));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!fieldIds.isEmpty()) {
            Map<Long, ClientTypeField> fieldMap = clientTypeFieldService.getFieldsByIds(fieldIds).stream()
                    .collect(Collectors.toMap(ClientTypeField::getId, field -> field));

            for (String field : selectedFields) {
                if (field.startsWith("field_")) {
                    try {
                        Long fieldId = Long.parseLong(field.substring(6));
                        ClientTypeField clientTypeField = fieldMap.get(fieldId);
                        if (clientTypeField != null) {
                            headerMap.put(field, clientTypeField.getFieldLabel());
                        } else {
                            log.warn("Field not found for field ID: {}", fieldId);
                            headerMap.put(field, field);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Invalid field ID in field name {}: {}", field, e.getMessage());
                        headerMap.put(field, field);
                    }
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

    private void fillDataRows(Sheet sheet, List<Client> clientList, List<String> selectedFields, FilterIds filterIds) {
        int rowIndex = 1;
        for (Client client : clientList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(client, field, filterIds));
            }
        }
    }

    private String getFieldValue(Client client, String field, FilterIds filterIds) {
        if (field.startsWith("field_")) {
            try {
                Long fieldId = Long.parseLong(field.substring(6));
                return getDynamicFieldValue(client, fieldId);
            } catch (Exception e) {
                log.warn("Failed to get dynamic field value for field {}: {}", field, e.getMessage());
                return "";
            }
        }

        return switch (field) {
            case "id" -> client.getId() != null ? String.valueOf(client.getId()) : "";
            case "company" -> client.getCompany() != null ? client.getCompany() : "";
            case "createdAt" -> client.getCreatedAt() != null ? client.getCreatedAt().toString() : "";
            case "updatedAt" -> client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : "";
            case "source" -> {
                if (client.getSource() == null) {
                    yield "";
                }
                yield filterIds.sourceDTOs().stream()
                        .filter(source -> source.getId() != null && source.getId().equals(client.getSource()))
                        .findFirst()
                        .map(Source::getName)
                        .orElse("");
            }
            default -> "";
        };
    }
    
    private String getDynamicFieldValue(Client client, Long fieldId) {
        if (client.getFieldValues() == null || client.getFieldValues().isEmpty()) {
            return "";
        }
        
        List<ClientFieldValue> fieldValues = client.getFieldValues().stream()
                .filter(fv -> fv.getField() != null && fv.getField().getId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .collect(Collectors.toList());
        
        if (fieldValues.isEmpty()) {
            return "";
        }
        
        ClientTypeField field = fieldValues.get(0).getField();
        if (field == null) {
            return "";
        }

        if (field.getAllowMultiple() != null && field.getAllowMultiple() && fieldValues.size() > 1) {
            return fieldValues.stream()
                    .map(fv -> formatFieldValue(fv, field))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
        } else {
            return formatFieldValue(fieldValues.get(0), field);
        }
    }
    
    private String formatFieldValue(ClientFieldValue fieldValue, ClientTypeField field) {
        if (field == null) {
            return "";
        }
        
        return switch (field.getFieldType()) {
            case TEXT, PHONE -> fieldValue.getValueText() != null ? fieldValue.getValueText() : "";
            case NUMBER -> fieldValue.getValueNumber() != null ? String.valueOf(fieldValue.getValueNumber()) : "";
            case DATE -> fieldValue.getValueDate() != null ? fieldValue.getValueDate().toString() : "";
            case BOOLEAN -> {
                if (fieldValue.getValueBoolean() == null) yield "";
                yield fieldValue.getValueBoolean() ? "Так" : "Ні";
            }
            case LIST -> {
                if (fieldValue.getValueList() != null && fieldValue.getValueList().getValue() != null) {
                    yield fieldValue.getValueList().getValue();
                }
                yield "";
            }
        };
    }

    private byte[] convertWorkbookToBytes(Workbook workbook) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ClientException("EXCEL_GENERATION_ERROR",
                    String.format("Error generating Excel file: %s", e.getMessage()));
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                log.warn("Failed to close workbook: {}", e.getMessage());
            }
        }
    }
}

