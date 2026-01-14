package org.example.containerservice.services;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.containerservice.clients.ClientApiClient;
import org.example.containerservice.clients.ClientTypeFieldApiClient;
import org.example.containerservice.clients.UserApiClient;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.UserDTO;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.containerservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.containerservice.models.dto.fields.SourceDTO;
import org.example.containerservice.models.dto.impl.IdNameDTO;
import feign.FeignException;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.services.impl.IClientContainerSpecialOperationsService;
import org.example.containerservice.spec.ClientContainerSpecification;
import org.example.containerservice.utils.FilterUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerSpecialOperationsService implements IClientContainerSpecialOperationsService {

    private static final int MAX_QUERY_LENGTH = 255;
    private static final String ERROR_INVALID_QUERY = "INVALID_QUERY";
    private static final String ERROR_INVALID_FIELDS = "INVALID_FIELDS";
    private static final String ERROR_EXCEL_GENERATION_ERROR = "EXCEL_GENERATION_ERROR";
    private static final String MESSAGE_QUERY_TOO_LONG = "Search query cannot exceed 255 characters";
    private static final String MESSAGE_FIELDS_EMPTY = "The list of fields for export cannot be empty";
    private static final String MESSAGE_EXCEL_ERROR = "Error generating Excel file: %s";
    private static final String FIELD_PREFIX = "field_";
    private static final int FIELD_PREFIX_LENGTH = 6;
    private static final String FIELD_SUFFIX_CLIENT = "-client";
    private static final String FILTER_KEY_CLIENT_PRODUCT = "clientProduct";
    private static final String FILTER_KEY_CLIENT_SOURCE = "clientSource";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_FROM = "clientCreatedAtFrom";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_TO = "clientCreatedAtTo";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_FROM = "clientUpdatedAtFrom";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_TO = "clientUpdatedAtTo";
    private static final String MAPPED_KEY_SOURCE = "source";
    private static final String MAPPED_KEY_CREATED_AT_FROM = "createdAtFrom";
    private static final String MAPPED_KEY_CREATED_AT_TO = "createdAtTo";
    private static final String MAPPED_KEY_UPDATED_AT_FROM = "updatedAtFrom";
    private static final String MAPPED_KEY_UPDATED_AT_TO = "updatedAtTo";
    private static final String FIELD_TYPE_TEXT = "TEXT";
    private static final String FIELD_TYPE_PHONE = "PHONE";
    private static final String FIELD_TYPE_NUMBER = "NUMBER";
    private static final String FIELD_TYPE_DATE = "DATE";
    private static final String FIELD_TYPE_BOOLEAN = "BOOLEAN";
    private static final String FIELD_TYPE_LIST = "LIST";
    private static final String BOOLEAN_TRUE_UA = "Так";
    private static final String BOOLEAN_FALSE_UA = "Ні";
    private static final String EMPTY_STRING = "";
    private static final String SHEET_NAME = "Container Data";
    private static final String FILENAME_PREFIX = "container_data_";
    private static final String FILENAME_SUFFIX = ".xlsx";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_DISPOSITION_TEMPLATE = "attachment; filename=%s";
    private static final String FIELD_ID_CLIENT = "id-client";
    private static final String FIELD_COMPANY_CLIENT = "company-client";
    private static final String FIELD_CREATED_AT_CLIENT = "createdAt-client";
    private static final String FIELD_UPDATED_AT_CLIENT = "updatedAt-client";
    private static final String FIELD_SOURCE_CLIENT = "source-client";
    private static final String FIELD_ID = "id";
    private static final String FIELD_USER = "user";
    private static final String FIELD_CONTAINER = "container";
    private static final String FIELD_QUANTITY = "quantity";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String VALUE_SEPARATOR = ", ";

    private final ClientContainerRepository clientContainerRepository;
    private final ClientApiClient clientApiClient;
    private final ClientTypeFieldApiClient clientTypeFieldApiClient;
    private final UserApiClient userClient;

    @Override
    @Transactional(readOnly = true)
    public void generateExcelFile(Sort.Direction sortDirection,
                                  String sortProperty,
                                  String query,
                                  Map<String, List<String>> filterParams,
                                  @NonNull HttpServletResponse response,
                                  @NonNull List<String> selectedFields) {
        validateInputs(query, selectedFields);

        Sort sort = createSort(sortDirection, sortProperty);
        FilterIds filterIds = fetchFilterIds();

        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        if (clients.isEmpty()) {
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        List<Long> clientIds = clients.stream()
                .map(ClientDTO::getId)
                .filter(Objects::nonNull)
                .toList();
        List<ClientContainer> clientContainerList = fetchClientContainers(query, filterParams, clientIds, sort);
        Map<Long, ClientDTO> clientMap = fetchClientMap(clients);

        FilterUtils.extractClientTypeId(filterParams);
        Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap = fetchClientFieldValues(clientIds);
        List<SourceDTO> sourceDTOs = fetchClientSourceDTOs(clients);

        FilterIds updatedFilterIds = new FilterIds(
                filterIds.userDTOs(),
                filterIds.userIds(),
                sourceDTOs
        );

        Workbook workbook = generateWorkbook(clientContainerList, selectedFields, updatedFilterIds, clientMap,
                clientFieldValuesMap);

        sendExcelFileResponse(workbook, response);
    }

    private record FilterIds(
            List<UserDTO> userDTOs, List<Long> userIds, List<SourceDTO> sourceDTOs
    ) {}

    private void validateInputs(String query, @NonNull List<String> selectedFields) {
        validateQuery(query);
        validateSelectedFields(selectedFields);
    }

    private void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ContainerException(ERROR_INVALID_QUERY, MESSAGE_QUERY_TOO_LONG);
        }
    }

    private void validateSelectedFields(@NonNull List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            throw new ContainerException(ERROR_INVALID_FIELDS, MESSAGE_FIELDS_EMPTY);
        }
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private FilterIds fetchFilterIds() {
        List<UserDTO> userDTOs = userClient.getAllUsers().getBody();
        if (userDTOs == null) {
            userDTOs = Collections.emptyList();
        }
        List<Long> userIds = userDTOs.stream().map(UserDTO::getId).toList();

        return new FilterIds(userDTOs, userIds, Collections.emptyList());
    }

    private List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);

        Map<String, List<String>> filteredParams = filterParams != null
                ? filterParams.entrySet().stream()
                        .filter(entry -> isClientFilterKey(entry.getKey()))
                        .collect(Collectors.toMap(
                                entry -> mapClientFilterKey(entry.getKey()),
                                Map.Entry::getValue))
                : Collections.emptyMap();

        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
        List<ClientDTO> clients = clientApiClient.searchClients(clientRequest).getBody();
        return clients != null ? clients : Collections.emptyList();
    }

    private boolean isClientFilterKey(String key) {
        return FILTER_KEY_CLIENT_PRODUCT.equals(key) ||
                FILTER_KEY_CLIENT_SOURCE.equals(key) ||
                FILTER_KEY_CLIENT_CREATED_AT_FROM.equals(key) ||
                FILTER_KEY_CLIENT_CREATED_AT_TO.equals(key) ||
                FILTER_KEY_CLIENT_UPDATED_AT_FROM.equals(key) ||
                FILTER_KEY_CLIENT_UPDATED_AT_TO.equals(key) ||
                key.startsWith(FIELD_PREFIX);
    }

    private String mapClientFilterKey(String key) {
        return switch (key) {
            case FILTER_KEY_CLIENT_SOURCE -> MAPPED_KEY_SOURCE;
            case FILTER_KEY_CLIENT_CREATED_AT_FROM -> MAPPED_KEY_CREATED_AT_FROM;
            case FILTER_KEY_CLIENT_CREATED_AT_TO -> MAPPED_KEY_CREATED_AT_TO;
            case FILTER_KEY_CLIENT_UPDATED_AT_FROM -> MAPPED_KEY_UPDATED_AT_FROM;
            case FILTER_KEY_CLIENT_UPDATED_AT_TO -> MAPPED_KEY_UPDATED_AT_TO;
            default -> key;
        };
    }

    private Map<Long, ClientDTO> fetchClientMap(@NonNull List<ClientDTO> clients) {
        return clients.stream()
                .filter(client -> client.getId() != null)
                .collect(Collectors.toMap(ClientDTO::getId, client -> client));
    }

    private List<ClientContainer> fetchClientContainers(String query,
                                                         Map<String, List<String>> filterParams,
                                                         @NonNull List<Long> clientIds,
                                                         @NonNull Sort sort) {
        Specification<ClientContainer> spec = (root, querySpec, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (clientIds.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            predicates.add(root.get("client").in(clientIds));

            Specification<ClientContainer> clientContainerSpec =
                    new ClientContainerSpecification(query, filterParams, clientIds);
            predicates.add(clientContainerSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return clientContainerRepository.findAll(spec, sort);
    }

    private Workbook generateWorkbook(@NonNull List<ClientContainer> clientContainerList,
                                      @NonNull List<String> selectedFields,
                                      @NonNull FilterIds filterIds,
                                      @NonNull Map<Long, ClientDTO> clientMap,
                                      @NonNull Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(SHEET_NAME);

        List<String> sortedFields = sortFields(selectedFields);
        Map<String, String> fieldToHeader = createFieldToHeaderMap(sortedFields);
        createHeaderRow(sheet, sortedFields, fieldToHeader);
        fillDataRows(sheet, clientContainerList, sortedFields, filterIds, clientMap, clientFieldValuesMap);

        return workbook;
    }
    
    private List<String> sortFields(@NonNull List<String> selectedFields) {
        List<String> clientFields = new ArrayList<>();
        List<String> containerFields = new ArrayList<>();

        for (String field : selectedFields) {
            if (field.endsWith(FIELD_SUFFIX_CLIENT) || field.startsWith(FIELD_PREFIX)) {
                clientFields.add(field);
            } else {
                containerFields.add(field);
            }
        }

        List<String> sorted = new ArrayList<>(clientFields);
        sorted.addAll(containerFields);
        return sorted;
    }

    private Map<String, String> createFieldToHeaderMap(@NonNull List<String> selectedFields) {
        Map<String, String> headerMap = createStaticHeaderMap();

        List<Long> fieldIds = extractFieldIds(selectedFields);
        if (!fieldIds.isEmpty()) {
            Map<Long, ClientTypeFieldDTO> fieldMap = loadFieldMap(fieldIds);
            addDynamicFieldHeaders(headerMap, selectedFields, fieldMap);
        }

        return headerMap;
    }

    private Map<String, String> createStaticHeaderMap() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(FIELD_ID_CLIENT, "Id (клієнта)");
        headerMap.put(FIELD_COMPANY_CLIENT, "Компанія (клієнта)");
        headerMap.put(FIELD_CREATED_AT_CLIENT, "Дата створення (клієнта)");
        headerMap.put(FIELD_UPDATED_AT_CLIENT, "Дата оновлення (клієнта)");
        headerMap.put(FIELD_SOURCE_CLIENT, "Залучення (клієнта)");
        headerMap.put(FIELD_ID, "Id");
        headerMap.put(FIELD_USER, "Власник");
        headerMap.put(FIELD_CONTAINER, "Тип тари");
        headerMap.put(FIELD_QUANTITY, "Кількість");
        headerMap.put(FIELD_UPDATED_AT, "Дата оновлення");
        return headerMap;
    }

    private List<Long> extractFieldIds(@NonNull List<String> selectedFields) {
        return selectedFields.stream()
                .filter(field -> field.startsWith(FIELD_PREFIX))
                .map(this::parseFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Long parseFieldId(@NonNull String field) {
        try {
            return Long.parseLong(field.substring(FIELD_PREFIX_LENGTH));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private Map<Long, ClientTypeFieldDTO> loadFieldMap(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ClientTypeFieldDTO> result = new HashMap<>();
        int batchSize = 100;

        for (int i = 0; i < fieldIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, fieldIds.size());
            List<Long> batch = fieldIds.subList(i, endIndex);

            try {
                org.example.containerservice.models.dto.clienttype.FieldIdsRequest request =
                        new org.example.containerservice.models.dto.clienttype.FieldIdsRequest(batch);
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

    private void addDynamicFieldHeaders(@NonNull Map<String, String> headerMap,
                                       @NonNull List<String> selectedFields,
                                       @NonNull Map<Long, ClientTypeFieldDTO> fieldMap) {
        for (String field : selectedFields) {
            if (field.startsWith(FIELD_PREFIX)) {
                Long fieldId = parseFieldId(field);
                if (fieldId != null) {
                    ClientTypeFieldDTO fieldDTO = fieldMap.get(fieldId);
                    String header;
                    if (fieldDTO != null) {
                        String label = fieldDTO.getFieldLabel();
                        if (label != null && !label.trim().isEmpty()) {
                            header = label + " (клієнта)";
                        } else {
                            String name = fieldDTO.getFieldName();
                            header = (name != null && !name.trim().isEmpty())
                                    ? name + " (клієнта)"
                                    : field + " (клієнта)";
                        }
                    } else {
                        header = field + " (клієнта)";
                    }
                    headerMap.put(field, header);
                } else {
                    headerMap.put(field, field + " (клієнта)");
                }
            }
        }
    }

    private void createHeaderRow(@NonNull Sheet sheet,
                                 @NonNull List<String> selectedFields,
                                 @NonNull Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            String header = fieldToHeader.getOrDefault(field, field);
            headerRow.createCell(colIndex++).setCellValue(header);
        }
    }

    private void fillDataRows(@NonNull Sheet sheet,
                             @NonNull List<ClientContainer> clientContainerList,
                             @NonNull List<String> selectedFields,
                             @NonNull FilterIds filterIds,
                             @NonNull Map<Long, ClientDTO> clientMap,
                             @NonNull Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        int rowIndex = 1;
        for (ClientContainer clientContainer : clientContainerList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(clientContainer.getClient());
            List<ClientFieldValueDTO> fieldValues = client != null
                    ? clientFieldValuesMap.getOrDefault(client.getId(), Collections.emptyList())
                    : Collections.emptyList();
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(clientContainer, client, field, filterIds, fieldValues));
            }
        }
    }

    private String getFieldValue(@NonNull ClientContainer clientContainer,
                                 ClientDTO client,
                                 @NonNull String field,
                                 @NonNull FilterIds filterIds,
                                 @NonNull List<ClientFieldValueDTO> fieldValues) {
        if (field.startsWith(FIELD_PREFIX)) {
            Long fieldId = parseFieldId(field);
            if (fieldId != null) {
                return getDynamicFieldValue(fieldValues, fieldId);
            }
            return EMPTY_STRING;
        }

        if (field.endsWith(FIELD_SUFFIX_CLIENT) && client != null) {
            return getClientFieldValue(client, field, filterIds);
        }

        return getContainerFieldValue(clientContainer, field, filterIds);
    }

    private String getClientFieldValue(@NonNull ClientDTO client, @NonNull String field, @NonNull FilterIds filterIds) {
        return switch (field) {
            case FIELD_ID_CLIENT -> client.getId() != null ? String.valueOf(client.getId()) : EMPTY_STRING;
            case FIELD_COMPANY_CLIENT -> client.getCompany() != null ? client.getCompany() : EMPTY_STRING;
            case FIELD_CREATED_AT_CLIENT -> client.getCreatedAt() != null ? client.getCreatedAt() : EMPTY_STRING;
            case FIELD_UPDATED_AT_CLIENT -> client.getUpdatedAt() != null ? client.getUpdatedAt() : EMPTY_STRING;
            case FIELD_SOURCE_CLIENT -> getClientSourceName(client, filterIds.sourceDTOs());
            default -> EMPTY_STRING;
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
            return EMPTY_STRING;
        }
        try {
            Long sourceIdLong = Long.parseLong(sourceId.trim());
            return getNameFromDTOList(sourceDTOs, sourceIdLong);
        } catch (NumberFormatException e) {
            return EMPTY_STRING;
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

        }

        return new ArrayList<>(uniqueSources.values());
    }

    private String getContainerFieldValue(@NonNull ClientContainer clientContainer,
                                         @NonNull String field,
                                         @NonNull FilterIds filterIds) {
        return switch (field) {
            case FIELD_ID -> clientContainer.getId() != null ? String.valueOf(clientContainer.getId()) : EMPTY_STRING;
            case FIELD_USER -> getNameFromDTOList(filterIds.userDTOs(), clientContainer.getUser());
            case FIELD_CONTAINER -> clientContainer.getContainer().getName();
            case FIELD_QUANTITY -> clientContainer.getQuantity() != null
                    ? clientContainer.getQuantity().toString() : EMPTY_STRING;
            case FIELD_UPDATED_AT -> clientContainer.getUpdatedAt() != null
                    ? clientContainer.getUpdatedAt().toString() : EMPTY_STRING;
            default -> EMPTY_STRING;
        };
    }
    
    private String getDynamicFieldValue(@NonNull List<ClientFieldValueDTO> fieldValues, @NonNull Long fieldId) {
        if (fieldValues.isEmpty()) {
            return EMPTY_STRING;
        }

        List<ClientFieldValueDTO> matchingValues = fieldValues.stream()
                .filter(fv -> fv.getFieldId() != null && fv.getFieldId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .toList();

        if (matchingValues.isEmpty()) {
            return EMPTY_STRING;
        }

        ClientFieldValueDTO firstValue = matchingValues.getFirst();
        String fieldType = firstValue.getFieldType();

        if (matchingValues.size() > 1) {
            return matchingValues.stream()
                    .map(fv -> formatFieldValue(fv, fieldType))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(VALUE_SEPARATOR));
        }

        return formatFieldValue(firstValue, fieldType);
    }
    
    private String formatFieldValue(@NonNull ClientFieldValueDTO fieldValue, @NonNull String fieldType) {
        return switch (fieldType) {
            case FIELD_TYPE_TEXT, FIELD_TYPE_PHONE -> fieldValue.getValueText() != null
                    ? fieldValue.getValueText() : EMPTY_STRING;
            case FIELD_TYPE_NUMBER -> fieldValue.getValueNumber() != null
                    ? fieldValue.getValueNumber().toString() : EMPTY_STRING;
            case FIELD_TYPE_DATE -> fieldValue.getValueDate() != null
                    ? fieldValue.getValueDate().toString() : EMPTY_STRING;
            case FIELD_TYPE_BOOLEAN -> formatBooleanValue(fieldValue.getValueBoolean());
            case FIELD_TYPE_LIST -> fieldValue.getValueListValue() != null
                    ? fieldValue.getValueListValue() : EMPTY_STRING;
            default -> EMPTY_STRING;
        };
    }

    private String formatBooleanValue(Boolean value) {
        if (value == null) {
            return EMPTY_STRING;
        }
        return value ? BOOLEAN_TRUE_UA : BOOLEAN_FALSE_UA;
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
                org.example.containerservice.models.dto.client.ClientIdsRequest request =
                        new org.example.containerservice.models.dto.client.ClientIdsRequest(batch);
                Map<Long, List<ClientFieldValueDTO>> batchResult = clientApiClient.getClientFieldValuesBatch(request).getBody();
                if (batchResult != null) {
                    result.putAll(batchResult);
                }
            } catch (FeignException e) {
                log.error("Feign error fetching field values batch for clients: status={}, error={}",
                        e.status(), e.getMessage(), e);
            } catch (Exception e) {
                log.warn("Failed to fetch field values batch for clients: {}", e.getMessage());
            }
        }

        return result;
    }

    private <T extends IdNameDTO> String getNameFromDTOList(@NonNull List<T> dtoList, Long id) {
        if (id == null) {
            return EMPTY_STRING;
        }
        return dtoList.stream()
                .filter(dto -> dto.getId() != null && dto.getId().equals(id))
                .findFirst()
                .map(IdNameDTO::getName)
                .orElse(EMPTY_STRING);
    }

    private void sendExcelFileResponse(@NonNull Workbook workbook, @NonNull HttpServletResponse response) {
        try {
            response.setContentType(CONTENT_TYPE);
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
            String filename = FILENAME_PREFIX + dateStr + FILENAME_SUFFIX;
            response.setHeader("Content-Disposition", String.format(CONTENT_DISPOSITION_TEMPLATE, filename));
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            try {
                workbook.close();
            } catch (IOException closeException) {
                log.warn("Failed to close workbook: {}", closeException.getMessage());
            }
            throw new ContainerException(ERROR_EXCEL_GENERATION_ERROR,
                    String.format(MESSAGE_EXCEL_ERROR, e.getMessage()));
        }
    }
}

