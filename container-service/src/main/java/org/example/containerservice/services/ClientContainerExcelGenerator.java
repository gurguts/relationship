package org.example.containerservice.services;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.containerservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.containerservice.services.ClientContainerExportDataFetcher.FilterIds;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClientContainerExcelGenerator {

    private static final String SHEET_NAME = "Container Data";
    private static final String FIELD_PREFIX = "field_";
    private static final String FIELD_SUFFIX_CLIENT = "-client";
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

    private final ClientContainerExportDataFetcher dataFetcher;
    private final ClientContainerFieldValueFormatter fieldValueFormatter;

    public Workbook generateWorkbook(@NonNull List<ClientContainer> clientContainerList,
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
            Map<Long, ClientTypeFieldDTO> fieldMap = dataFetcher.loadFieldMap(fieldIds);
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
            return Long.parseLong(field.substring(6)); // FIELD_PREFIX_LENGTH = 6
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    private void addDynamicFieldHeaders(@NonNull Map<String, String> headerMap,
                                       @NonNull List<String> selectedFields,
                                       @NonNull Map<Long, ClientTypeFieldDTO> fieldMap) {
        for (String field : selectedFields) {
            if (field.startsWith(FIELD_PREFIX)) {
                Long fieldId = parseFieldId(field);
                if (fieldId != null) {
                    ClientTypeFieldDTO fieldDTO = fieldMap.get(fieldId);
                    String header = getHeader(field, fieldDTO);
                    headerMap.put(field, header);
                } else {
                    headerMap.put(field, field + " (клієнта)");
                }
            }
        }
    }

    @NotNull
    private static String getHeader(String field, ClientTypeFieldDTO fieldDTO) {
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
        return header;
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
                row.createCell(colIndex++).setCellValue(
                        fieldValueFormatter.getFieldValue(clientContainer, client, field, filterIds, fieldValues));
            }
        }
    }
}
