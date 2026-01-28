package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.dto.clienttype.FieldIdsRequest;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.IClientTypeFieldService;
import org.example.clientservice.services.impl.ISourceService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientExcelGenerator {

    private static final String FIELD_PREFIX = "field_";

    private final IClientTypeFieldService clientTypeFieldService;
    private final ISourceService sourceService;
    private final ClientFieldValueFormatter fieldValueFormatter;
    private final ClientExportValidator validator;

    public Workbook generateWorkbook(@NonNull List<Client> clientList, @NonNull List<String> selectedFields) {
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
                Long fieldId = validator.parseFieldIdFromString(field);
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

    private List<Long> extractFieldIds(@NonNull List<String> selectedFields) {
        return selectedFields.stream()
                .filter(field -> field.startsWith(FIELD_PREFIX))
                .map(validator::parseFieldIdFromString)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<Long, ClientTypeField> loadFieldMap(@NonNull List<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        FieldIdsRequest request = new FieldIdsRequest(fieldIds);
        return clientTypeFieldService.getFieldsByIds(request).stream()
                .collect(Collectors.toMap(ClientTypeField::getId, field -> field));
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
                row.createCell(colIndex++).setCellValue(
                        fieldValueFormatter.getFieldValue(client, field, sourceMap));
            }
        }
    }
}
