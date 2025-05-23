package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.PhoneNumber;
import org.example.clientservice.models.field.*;
import org.example.clientservice.models.field.StatusClient;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSpecialOperationsService implements IClientSpecialOperationsService {

    private static final Set<String> VALID_FIELDS = Set.of(
            "id", "company", "person", "phoneNumbers", "createdAt", "updatedAt", "status", "source",
            "location", "pricePurchase", "priceSale", "volumeMonth", "route", "region", "business",
            "edrpou", "enterpriseName", "vat", "comment");


    private final ClientRepository clientRepository;
    private final IBusinessService businessService;
    private final IRegionService regionService;
    private final IRouteService routeService;
    private final ISourceService sourceService;
    private final IStatusClientService statusClientService;

    @Override
    public byte[] generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            List<String> selectedFields,
            String excludedStatuses
    ) {
        validateInputs(query, filterParams, selectedFields);

        Sort sort = createSort(sortDirection, sortProperty);
        List<Long> excludeStatusIds = parseExcludedStatuses(excludedStatuses);
        FilterIds filterIds = fetchFilterIds();

        List<Client> clientList = fetchClients(query, filterParams, filterIds, excludeStatusIds, sort);

        Workbook workbook = generateWorkbook(clientList, selectedFields, filterIds);

        return convertWorkbookToBytes(workbook);
    }

    private void validateInputs(String query, Map<String, List<String>> filterParams, List<String> selectedFields) {
        if (query != null && query.length() > 255) {
            throw new ClientException("INVALID_QUERY", "Search query cannot exceed 255 characters");
        }
        if (filterParams != null) {
            Set<String> validKeys = Set.of("createdAtFrom", "createdAtTo", "updatedAtFrom", "updated pisAtTo",
                    "business", "route", "region", "status", "source");
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
            List<Business> businessDTOs, List<Long> businessIds,
            List<Region> regionDTOs, List<Long> regionIds,
            List<Route> routeDTOs, List<Long> routeIds,
            List<Source> sourceDTOs, List<Long> sourceIds,
            List<StatusClient> statusDTOs, List<Long> statusIds
    ) {
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private List<Long> parseExcludedStatuses(String excludedStatuses) {
        if (excludedStatuses == null || excludedStatuses.trim().isEmpty()) {
            return null;
        }
        try {
            return Arrays.stream(excludedStatuses.split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException e) {
            throw new ClientException("INVALID_STATUSES", String.format("Incorrect status format for exclusion: %s",
                    excludedStatuses));
        }
    }

    private FilterIds fetchFilterIds() {
        List<Business> businessDTOs = businessService.getAllBusinesses();
        List<Long> businessIds = businessDTOs.stream().map(Business::getId).toList();

        List<Region> regionDTOs = regionService.getAllRegions();
        List<Long> regionIds = regionDTOs.stream().map(Region::getId).toList();

        List<Route> routeDTOs = routeService.getAllRoutes();
        List<Long> routeIds = routeDTOs.stream().map(Route::getId).toList();

        List<Source> sourceDTOs = sourceService.getAllSources();
        List<Long> sourceIds = sourceDTOs.stream().map(Source::getId).toList();

        List<StatusClient> statusDTOs = statusClientService.getAllStatusClients();
        List<Long> statusIds = statusDTOs.stream().map(StatusClient::getId).toList();

        return new FilterIds(businessDTOs, businessIds, regionDTOs, regionIds, routeDTOs, routeIds,
                sourceDTOs, sourceIds, statusDTOs, statusIds);
    }

    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      List<Long> excludeStatusIds, Sort sort) {
        return clientRepository.findAll(new ClientSpecification(
                query,
                filterParams,
                filterIds.statusIds(),
                filterIds.sourceIds(),
                filterIds.routeIds(),
                filterIds.regionIds(),
                filterIds.businessIds(),
                excludeStatusIds
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
            case "person" -> client.getPerson() != null ? client.getPerson() : "";
            case "phoneNumbers" -> client.getPhoneNumbers() != null
                    ? client.getPhoneNumbers().stream().map(PhoneNumber::getNumber)
                    .collect(Collectors.joining(", "))
                    : "";
            case "createdAt" -> client.getCreatedAt() != null ? client.getCreatedAt().toString() : "";
            case "updatedAt" -> client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : "";
            case "location" -> client.getLocation() != null ? client.getLocation() : "";
            case "pricePurchase" -> client.getPricePurchase() != null ? client.getPricePurchase() : "";
            case "priceSale" -> client.getPriceSale() != null ? client.getPriceSale() : "";
            case "volumeMonth" -> client.getVolumeMonth() != null ? client.getVolumeMonth() : "";
            case "status" -> filterIds.statusDTOs().stream()
                    .filter(status -> status.getId().equals(client.getStatus()))
                    .findFirst()
                    .map(StatusClient::getName)
                    .orElse("");
            case "source" -> filterIds.sourceDTOs().stream()
                    .filter(source -> source.getId().equals(client.getSource()))
                    .findFirst()
                    .map(Source::getName)
                    .orElse("");
            case "route" -> filterIds.routeDTOs().stream()
                    .filter(route -> route.getId().equals(client.getRoute()))
                    .findFirst()
                    .map(Route::getName)
                    .orElse("");
            case "region" -> filterIds.regionDTOs().stream()
                    .filter(region -> region.getId().equals(client.getRegion()))
                    .findFirst()
                    .map(Region::getName)
                    .orElse("");
            case "business" -> filterIds.businessDTOs().stream()
                    .filter(region -> region.getId().equals(client.getRegion()))
                    .findFirst()
                    .map(Business::getName)
                    .orElse("");
            case "edrpou" -> client.getEdrpou() != null ? client.getEdrpou() : "";
            case "enterpriseName" -> client.getEnterpriseName() != null ? client.getEnterpriseName() : "";
            case "vat" -> Boolean.TRUE.equals(client.getVat()) ? "так" : "";
            case "comment" -> client.getComment() != null ? client.getComment() : "";
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
