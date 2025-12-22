package org.example.containerservice.services;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.containerservice.clients.*;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.*;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.client.ClientSearchRequest;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.containerservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.containerservice.models.dto.fields.*;
import org.example.containerservice.models.dto.impl.IdNameDTO;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.services.impl.IClientContainerSpecialOperationsService;
import org.example.containerservice.spec.ClientContainerSpecification;
import org.example.containerservice.utils.FilterUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerSpecialOperationsService implements IClientContainerSpecialOperationsService {

    private final ClientContainerRepository clientContainerRepository;
    private final ClientApiClient clientApiClient;
    private final ClientTypeFieldApiClient clientTypeFieldApiClient;
    private final UserApiClient userClient;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            HttpServletResponse response,
            List<String> selectedFields) throws IOException {

        Sort sort = createSort(sortDirection, sortProperty);
        FilterIds filterIds = fetchFilterIds();

        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        if (clients.isEmpty()) {
            log.info("No clients found for the given filters, returning empty workbook");
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        List<ClientContainer> clientContainerList = fetchClientContainers(query,
                filterParams, clients.stream().map(ClientDTO::getId).toList(), sort);
        Map<Long, ClientDTO> clientMap = fetchClientMap(clients);
        
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);
        
        Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap = fetchClientFieldValues(clients.stream().map(ClientDTO::getId).toList());

        Workbook workbook = generateWorkbook(clientContainerList, selectedFields, filterIds, clientMap, clientTypeId, clientFieldValuesMap);

        sendExcelFileResponse(workbook, response);
    }

    private record FilterIds(
            List<UserDTO> userDTOs, List<Long> userIds
    ) {}

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private FilterIds fetchFilterIds() {
        List<UserDTO> userDTOs = userClient.getAllUsers();
        List<Long> userIds = userDTOs.stream().map(UserDTO::getId).toList();

        return new FilterIds(userDTOs, userIds);
    }

    private List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);
        
        Map<String, List<String>> filteredParams = filterParams != null ? filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("clientProduct") ||
                            key.equals("clientSource") ||
                            key.equals("clientCreatedAtFrom") || key.equals("clientCreatedAtTo") ||
                            key.equals("clientUpdatedAtFrom") || key.equals("clientUpdatedAtTo") ||
                            key.startsWith("field");
                })
                .collect(Collectors.toMap(
                    entry -> {
                        String key = entry.getKey();
                        if (key.equals("clientSource")) return "source";
                        if (key.equals("clientCreatedAtFrom")) return "createdAtFrom";
                        if (key.equals("clientCreatedAtTo")) return "createdAtTo";
                        if (key.equals("clientUpdatedAtFrom")) return "updatedAtFrom";
                        if (key.equals("clientUpdatedAtTo")) return "updatedAtTo";
                        return key;
                    },
                    Map.Entry::getValue
                )) : Collections.emptyMap();
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams, clientTypeId);
        return clientApiClient.searchClients(clientRequest);
    }

    private Map<Long, ClientDTO> fetchClientMap(List<ClientDTO> clients) {
        return clients.stream().collect(Collectors.toMap(ClientDTO::getId, client -> client));
    }

    private List<ClientContainer> fetchClientContainers(String query, Map<String,
            List<String>> filterParams, List<Long> clientIds, Sort sort) {

        Specification<ClientContainer> spec = (root, querySpec, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            } else {
                return criteriaBuilder.disjunction();
            }

            Specification<ClientContainer> clientContainerSpec =
                    new ClientContainerSpecification(query, filterParams, clientIds);
            predicates.add(clientContainerSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return clientContainerRepository.findAll(spec, sort);
    }

    private Workbook generateWorkbook(List<ClientContainer> clientContainerList,
                                      List<String> selectedFields, FilterIds filterIds,
                                      Map<Long, ClientDTO> clientMap, Long clientTypeId,
                                      Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Container Data");

        List<String> sortedFields = sortFields(selectedFields);
        Map<String, String> fieldToHeader = createFieldToHeaderMap(sortedFields, clientTypeId);
        createHeaderRow(sheet, sortedFields, fieldToHeader);
        fillDataRows(sheet, clientContainerList, sortedFields, filterIds, clientMap, clientFieldValuesMap);

        return workbook;
    }
    
    private List<String> sortFields(List<String> selectedFields) {
        List<String> clientFields = new ArrayList<>();
        List<String> containerFields = new ArrayList<>();
        
        for (String field : selectedFields) {
            if (field.endsWith("-client") || field.startsWith("field_")) {
                clientFields.add(field);
            } else {
                containerFields.add(field);
            }
        }
        
        List<String> sorted = new ArrayList<>(clientFields);
        sorted.addAll(containerFields);
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
        headerMap.put("user", "Власник");
        headerMap.put("container", "Тип тари");
        headerMap.put("quantity", "Кількість");
        headerMap.put("updatedAt", "Дата оновлення");
        
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
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ClientTypeFieldDTO> fieldMap = new HashMap<>();
        for (Long fieldId : fieldIds) {
            try {
                ClientTypeFieldDTO fieldDTO = clientTypeFieldApiClient.getFieldById(fieldId);
                if (fieldDTO != null) {
                    fieldMap.put(fieldId, fieldDTO);
                }
            } catch (Exception e) {
                log.warn("Failed to get field label for field {}: {}", fieldId, e.getMessage());
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

    private void fillDataRows(Sheet sheet, List<ClientContainer> clientContainerList,
                              List<String> selectedFields, FilterIds filterIds,
                              Map<Long, ClientDTO> clientMap, Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        int rowIndex = 1;
        for (ClientContainer clientContainer : clientContainerList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(clientContainer.getClient());
            List<ClientFieldValueDTO> fieldValues = client != null ? clientFieldValuesMap.getOrDefault(client.getId(), Collections.emptyList()) : Collections.emptyList();
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(clientContainer, client, field, filterIds, fieldValues));
            }
        }
    }

    private String getFieldValue(ClientContainer clientContainer, ClientDTO client, String field, FilterIds filterIds,
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
                case "source-client" -> client.getSource() != null ? client.getSource().getName() : "";
                default -> "";
            };
        } else {
            return switch (field) {
                case "id" -> clientContainer.getId() != null ? String.valueOf(clientContainer.getId()) : "";
                case "user" -> getNameFromDTOList(filterIds.userDTOs(), clientContainer.getUser());
                case "container" -> clientContainer.getContainer().getName();
                case "quantity" ->
                        clientContainer.getQuantity() != null ? clientContainer.getQuantity().toString() : "";
                case "updatedAt" ->
                        clientContainer.getUpdatedAt() != null ? clientContainer.getUpdatedAt().toString() : "";
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
            Map<Long, List<ClientFieldValueDTO>> result = clientApiClient.getClientFieldValuesBatch(clientIds);
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

    private void sendExcelFileResponse(Workbook workbook, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String filename = "container_data_" + dateStr + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}

