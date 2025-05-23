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
import org.example.containerservice.models.dto.fields.*;
import org.example.containerservice.models.dto.impl.IdNameDTO;
import org.example.containerservice.repositories.ClientContainerRepository;
import org.example.containerservice.services.impl.IClientContainerSpecialOperationsService;
import org.example.containerservice.spec.ClientContainerSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerSpecialOperationsService implements IClientContainerSpecialOperationsService {

    private final ClientContainerRepository clientContainerRepository;
    private final ClientApiClient clientApiClient;
    private final SourceClient sourceClient;
    private final ProductClient productClient;
    private final UserApiClient userClient;
    private final StatusClient statusClient;
    private final RouteClient routeClient;
    private final RegionClient regionClient;
    private final BusinessClient businessClient;

    @Override
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

        Workbook workbook = generateWorkbook(clientContainerList, selectedFields, filterIds, clientMap);

        sendExcelFileResponse(workbook, response);
    }

    private record FilterIds(
            List<SourceDTO> sourceDTOs, List<Long> sourceIds,
            List<StatusDTO> statusDTOs, List<Long> statusIds,
            List<RouteDTO> routeDTOs, List<Long> routeIds,
            List<RegionDTO> regionDTOs, List<Long> regionIds,
            List<BusinessDTO> businessDTOs, List<Long> businessIds,
            List<ProductDTO> productDTOs, List<Long> productIds,
            List<UserDTO> userDTOs, List<Long> userIds
    ) {
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private FilterIds fetchFilterIds() {
        List<SourceDTO> sourceDTOs = sourceClient.getAllSource();
        List<Long> sourceIds = sourceDTOs.stream().map(SourceDTO::getId).toList();

        List<StatusDTO> statusDTOs = statusClient.getAllStatus();
        List<Long> statusIds = statusDTOs.stream().map(StatusDTO::getId).toList();

        List<RouteDTO> routeDTOs = routeClient.getAllRoute();
        List<Long> routeIds = routeDTOs.stream().map(RouteDTO::getId).toList();

        List<RegionDTO> regionDTOs = regionClient.getAllRegion();
        List<Long> regionIds = regionDTOs.stream().map(RegionDTO::getId).toList();

        List<BusinessDTO> businessDTOs = businessClient.getAllBusiness();
        List<Long> businessIds = businessDTOs.stream().map(BusinessDTO::getId).toList();

        List<ProductDTO> productDTOs = productClient.getAllProduct();
        List<Long> productIds = productDTOs.stream().map(ProductDTO::getId).toList();

        List<UserDTO> userDTOs = userClient.getAllUsers();
        List<Long> userIds = userDTOs.stream().map(UserDTO::getId).toList();

        return new FilterIds(
                sourceDTOs, sourceIds,
                statusDTOs, statusIds,
                routeDTOs, routeIds,
                regionDTOs, regionIds,
                businessDTOs, businessIds,
                productDTOs, productIds,
                userDTOs, userIds);
    }

    private List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        Map<String, List<String>> filteredParams = filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("status") || key.equals("business") ||
                            key.equals("route") || key.equals("region") || key.equals("source-client");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams);
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
                                      Map<Long, ClientDTO> clientMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sale Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap();
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, clientContainerList, selectedFields, filterIds, clientMap);

        return workbook;
    }

    private Map<String, String> createFieldToHeaderMap() {
        return Map.ofEntries(
                Map.entry("id-client", "Id (клієнта)"),
                Map.entry("company-client", "Компанія (клієнта)"),
                Map.entry("person-client", "Контактна особа (клієнта)"),
                Map.entry("phoneNumbers-client", "Номери телефонів (клієнта)"),
                Map.entry("createdAt-client", "Дата створення (клієнта)"),
                Map.entry("updatedAt-client", "Дата оновлення (клієнта)"),
                Map.entry("status-client", "Статус (клієнта)"),
                Map.entry("source-client", "Залучення (клієнта)"),
                Map.entry("location-client", "Адреса (клієнта)"),
                Map.entry("pricePurchase-client", "Ціна закупівлі (клієнта)"),
                Map.entry("priceSale-client", "Ціна продажі (клієнта)"),
                Map.entry("volumeMonth-client", "Орієнтований об'єм на місяць (клієнта)"),
                Map.entry("route-client", "Маршрут (клієнта)"),
                Map.entry("region-client", "Область (клієнта)"),
                Map.entry("business-client", "Тип бізнесу (клієнта)"),
                Map.entry("edrpou-client", "ЄДРПОУ (клієнта)"),
                Map.entry("enterpriseName-client", "Назва підприємства (клієнта)"),
                Map.entry("vat-client", "ПДВ (клієнта)"),
                Map.entry("comment-client", "Коментар (клієнта)"),

                Map.entry("id", "Id"),
                Map.entry("user", "Власник"),
                Map.entry("container", "Тип тари"),
                Map.entry("quantity", "Кількість"),
                Map.entry("updatedAt", "Дата оновлення")
        );
    }

    private void createHeaderRow(Sheet sheet, List<String> selectedFields, Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            headerRow.createCell(colIndex++).setCellValue(fieldToHeader.get(field));
        }
    }

    private void fillDataRows(Sheet sheet, List<ClientContainer> clientContainerList,
                              List<String> selectedFields, FilterIds filterIds,
                              Map<Long, ClientDTO> clientMap) {
        int rowIndex = 1;
        for (ClientContainer clientContainer : clientContainerList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(clientContainer.getClient());
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(clientContainer, client, field, filterIds));
            }
        }
    }

    private String getFieldValue(ClientContainer clientContainer, ClientDTO client, String field, FilterIds filterIds) {
        if (field.endsWith("-client") && client != null) {
            return switch (field) {
                case "id-client" -> client.getId() != null ? String.valueOf(client.getId()) : "";
                case "company-client" -> client.getCompany() != null ? client.getCompany() : "";
                case "person-client" -> client.getPerson() != null ? client.getPerson() : "";
                case "phoneNumbers-client" -> client.getPhoneNumbers() != null
                        ? String.join(", ", client.getPhoneNumbers()) : "";
                case "createdAt-client" -> client.getCreatedAt() != null ? client.getCreatedAt() : "";
                case "updatedAt-client" -> client.getUpdatedAt() != null ? client.getUpdatedAt() : "";
                case "status-client" -> getNameFromDTOList(filterIds.statusDTOs(), client.getStatus() != null ?
                        safeParseLong(client.getStatus().getName()) : null);
                case "source-client" -> getNameFromDTOList(filterIds.sourceDTOs(), client.getSource() != null ?
                        safeParseLong(client.getSource().getName()) : null);
                case "location-client" -> client.getLocation() != null ? client.getLocation() : "";
                case "pricePurchase-client" -> client.getPricePurchase() != null ? client.getPricePurchase() : "";
                case "priceSale-client" -> client.getPriceSale() != null ? client.getPriceSale() : "";
                case "volumeMonth-client" -> client.getVolumeMonth() != null ? client.getVolumeMonth() : "";
                case "route-client" -> getNameFromDTOList(filterIds.routeDTOs(), client.getRoute() != null ?
                        safeParseLong(client.getRoute().getName()) : null);
                case "region-client" -> getNameFromDTOList(filterIds.regionDTOs(), client.getRegion() != null ?
                        safeParseLong(client.getRegion().getName()) : null);
                case "business-client" -> getNameFromDTOList(filterIds.businessDTOs(), client.getBusiness() != null ?
                        safeParseLong(client.getBusiness().getName()) : null);
                case "edrpou-client" -> client.getEdrpou() != null ? client.getEdrpou() : "";
                case "enterpriseName-client" -> client.getEnterpriseName() != null ? client.getEnterpriseName() : "";
                case "vat-client" -> Boolean.TRUE.equals(client.getVat()) ? "так" : "";
                case "comment-client" -> client.getComment() != null ? client.getComment() : "";
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

    private Long safeParseLong(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
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
        response.setHeader("Content-Disposition", "attachment; filename=clientContainer_data.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
