package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSpecialOperationsService implements IClientSpecialOperationsService {

    private static final Set<String> VALID_FIELDS = Set.of(
            "id", "company", "createdAt", "updatedAt", "source");


    private final ClientRepository clientRepository;
    private final ISourceService sourceService;

    @Override
    public byte[] generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            List<String> selectedFields
    ) {
        validateInputs(query, filterParams, selectedFields);

        Sort sort = createSort(sortDirection, sortProperty);
        FilterIds filterIds = (query != null && !query.trim().isEmpty()) ? fetchFilterIds(query) : fetchFilterIds();

        List<Client> clientList = fetchClients(query, filterParams, filterIds, sort);

        Workbook workbook = generateWorkbook(clientList, selectedFields, filterIds);

        return convertWorkbookToBytes(workbook);
    }

    private void validateInputs(String query, Map<String, List<String>> filterParams, List<String> selectedFields) {
        if (query != null && query.length() > 255) {
            throw new ClientException("INVALID_QUERY", "Search query cannot exceed 255 characters");
        }
        if (filterParams != null) {
            Set<String> validKeys = Set.of("createdAtFrom", "createdAtTo", "updatedAtFrom", "updatedAtTo", "source");
            for (String key : filterParams.keySet()) {
                if (!validKeys.contains(key)) {
                    throw new ClientException("INVALID_FILTER", String.format("Invalid filter key: %s", key));
                }
            }
        }
        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        if (!VALID_FIELDS.containsAll(selectedFields)) {
            throw new ClientException("INVALID_FIELDS", String.format("Invalid fields specified for export: %s",
                    selectedFields));
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

    private FilterIds fetchFilterIds() {
        List<Source> sourceDTOs = sourceService.getAllSources();
        List<Long> sourceIds = sourceDTOs.stream().map(Source::getId).toList();

        return new FilterIds(sourceDTOs, sourceIds);
    }

    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      Sort sort) {
        Long clientTypeId = filterParams != null && filterParams.containsKey("clientTypeId") 
            ? Long.parseLong(filterParams.get("clientTypeId").get(0)) 
            : null;

        return clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds.sourceIds(),
                clientTypeId
        ), sort);
    }

    private Workbook generateWorkbook(List<Client> clientList, List<String> selectedFields, FilterIds filterIds) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Client Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap();
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, clientList, selectedFields, filterIds);

        return workbook;
    }

    private Map<String, String> createFieldToHeaderMap() {
        return Map.ofEntries(
                Map.entry("id", "Id"),
                Map.entry("company", "Компанія"),
                Map.entry("person", "Контактна особа"),
                Map.entry("phoneNumbers", "Номери телефонів"),
                Map.entry("createdAt", "Дата створення"),
                Map.entry("updatedAt", "Дата оновлення"),
                Map.entry("status", "Статус"),
                Map.entry("source", "Залучення"),
                Map.entry("location", "Адреса"),
                Map.entry("pricePurchase", "Ціна закупівлі"),
                Map.entry("priceSale", "Ціна продажі"),
                Map.entry("volumeMonth", "Орієнтований об'єм на місяць"),
                Map.entry("route", "Маршрут"),
                Map.entry("region", "Область"),
                Map.entry("business", "Тип бізнесу"),
                Map.entry("clientProduct", "Товар"),
                Map.entry("edrpou", "ЄДРПОУ"),
                Map.entry("enterpriseName", "Назва підприємства"),
                Map.entry("vat", "ПДВ"),
                Map.entry("comment", "Коментар")
        );
    }

    private void createHeaderRow(Sheet sheet, List<String> selectedFields, Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            headerRow.createCell(colIndex++).setCellValue(fieldToHeader.get(field));
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
        return switch (field) {
            case "id" -> client.getId() != null ? String.valueOf(client.getId()) : "";
            case "company" -> client.getCompany() != null ? client.getCompany() : "";
            case "createdAt" -> client.getCreatedAt() != null ? client.getCreatedAt().toString() : "";
            case "updatedAt" -> client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : "";
            case "source" -> filterIds.sourceDTOs().stream()
                    .filter(source -> source.getId().equals(client.getSource()))
                    .findFirst()
                    .map(Source::getName)
                    .orElse("");
            default -> "";
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
