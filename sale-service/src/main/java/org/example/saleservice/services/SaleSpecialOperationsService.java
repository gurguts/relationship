package org.example.saleservice.services;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.saleservice.clients.*;
import org.example.saleservice.models.PaymentMethod;
import org.example.saleservice.models.Product;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.client.ClientDTO;
import org.example.saleservice.models.dto.client.ClientSearchRequest;
import org.example.saleservice.models.dto.fields.*;
import org.example.saleservice.models.dto.impl.IdNameDTO;
import org.example.saleservice.models.dto.user.UserDTO;
import org.example.saleservice.repositories.SaleRepository;
import org.example.saleservice.services.impl.IProductService;
import org.example.saleservice.services.impl.ISaleSpecialOperationsService;
import org.example.saleservice.spec.SaleSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleSpecialOperationsService implements ISaleSpecialOperationsService {

    private final SaleRepository saleRepository;
    private final ClientApiClient clientApiClient;
    private final IProductService productService;
    private final UserClient userClient;

    @Override
    public void generateExcelFile(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            HttpServletResponse response,
            List<String> selectedFields) throws IOException {

        Sort sort = createSort(sortDirection, sortProperty);
        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        FilterIds filterIds = fetchFilterIds(clients);

        if (clients.isEmpty()) {
            log.info("No clients found for the given filters, returning empty workbook");
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        List<Sale> saleList = fetchSales(
                query, filterParams, clients.stream().map(ClientDTO::getId).toList(), filterIds.sourceIds(), sort);
        Map<Long, ClientDTO> clientMap = fetchClientMap(clients);

        Workbook workbook = generateWorkbook(saleList, selectedFields, filterIds, clientMap);
        sendExcelFileResponse(workbook, response);
    }

    private record FilterIds(
            List<SourceDTO> sourceDTOs, List<Long> sourceIds,
            List<StatusDTO> statusDTOs, List<Long> statusIds,
            List<RouteDTO> routeDTOs, List<Long> routeIds,
            List<RegionDTO> regionDTOs, List<Long> regionIds,
            List<BusinessDTO> businessDTOs, List<Long> businessIds,
            List<ClientProductDTO> clientProductDTOs, List<Long> clientProductIds,
            List<Product> productDTOs, List<Long> productIds,
            List<UserDTO> userDTOs, List<Long> userIds
    ) {
    }

    private Sort createSort(Sort.Direction sortDirection, String sortProperty) {
        return Sort.by(sortDirection, sortProperty);
    }

    private FilterIds fetchFilterIds(List<ClientDTO> clients) {
        List<SourceDTO> sourceDTOs = clients.stream()
                .map(ClientDTO::getSource)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> sourceIds = sourceDTOs.stream().map(SourceDTO::getId).toList();

        List<StatusDTO> statusDTOs = clients.stream()
                .map(ClientDTO::getStatus)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> statusIds = statusDTOs.stream().map(StatusDTO::getId).toList();

        List<RouteDTO> routeDTOs = clients.stream()
                .map(ClientDTO::getRoute)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> routeIds = routeDTOs.stream().map(RouteDTO::getId).toList();

        List<RegionDTO> regionDTOs = clients.stream()
                .map(ClientDTO::getRegion)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> regionIds = regionDTOs.stream().map(RegionDTO::getId).toList();

        List<BusinessDTO> businessDTOs = clients.stream()
                .map(ClientDTO::getBusiness)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> businessIds = businessDTOs.stream().map(BusinessDTO::getId).toList();

        List<ClientProductDTO> clientProductDTOs = clients.stream()
                .map(ClientDTO::getClientProduct)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> clientProductIds = clientProductDTOs.stream().map(ClientProductDTO::getId).toList();

        List<Product> products = productService.getAllProducts("all");
        List<Long> productIds = products.stream().map(Product::getId).toList();

        List<UserDTO> userDTOs = userClient.getAllUsers();
        List<Long> userIds = userDTOs.stream().map(UserDTO::getId).toList();

        return new FilterIds(
                sourceDTOs, sourceIds,
                statusDTOs, statusIds,
                routeDTOs, routeIds,
                regionDTOs, regionIds,
                businessDTOs, businessIds,
                clientProductDTOs, clientProductIds,
                products, productIds,
                userDTOs, userIds);
    }

    private List<ClientDTO> fetchClientIds(String query, Map<String, List<String>> filterParams) {
        Map<String, List<String>> filteredParams = filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    return key.equals("status") || key.equals("business") ||
                            key.equals("route") || key.equals("region") || key.equals("source-client")||
                            key.equals("clientProduct");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams);
        return clientApiClient.searchClients(clientRequest);
    }

    private Map<Long, ClientDTO> fetchClientMap(List<ClientDTO> clients) {
        return clients.stream().collect(Collectors.toMap(ClientDTO::getId, client -> client));
    }

    private List<Sale> fetchSales(String query, Map<String, List<String>> filterParams, List<Long> clientIds,
                                  List<Long> sourceIds, Sort sort) {
        Specification<Sale> spec = (root, querySpec, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            } else {
                return criteriaBuilder.disjunction();
            }

            Specification<Sale> saleSpec = new SaleSpecification(query, filterParams, clientIds, sourceIds);
            predicates.add(saleSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return saleRepository.findAll(spec, sort);
    }

    private Workbook generateWorkbook(List<Sale> saleList, List<String> selectedFields, FilterIds filterIds,
                                      Map<Long, ClientDTO> clientMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sale Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap();
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, saleList, selectedFields, filterIds, clientMap);

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
                Map.entry("clientProduct-client", "Товар (клієнта)"),
                Map.entry("edrpou-client", "ЄДРПОУ (клієнта)"),
                Map.entry("enterpriseName-client", "Назва підприємства (клієнта)"),
                Map.entry("vat-client", "ПДВ (клієнта)"),
                Map.entry("comment-client", "Коментар (клієнта)"),
                Map.entry("id", "Id"),
                Map.entry("user", "Водій"),
                Map.entry("source", "Залучення"),
                Map.entry("product", "Товар"),
                Map.entry("quantity", "Кількість"),
                Map.entry("unitPrice", "Ціна за од"),
                Map.entry("totalPrice", "Повна ціна"),
                Map.entry("paymentMethod", "Метод оплати"),
                Map.entry("currency", "Валюта"),
                Map.entry("exchangeRate", "Курс"),
                Map.entry("transaction", "Id транзакції"),
                Map.entry("createdAt", "Дата створення"),
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

    private void fillDataRows(Sheet sheet, List<Sale> saleList, List<String> selectedFields, FilterIds filterIds,
                              Map<Long, ClientDTO> clientMap) {
        int rowIndex = 1;
        for (Sale sale : saleList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(sale.getClient());
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(sale, client, field, filterIds));
            }
        }
    }

    private String getFieldValue(Sale sale, ClientDTO client, String field, FilterIds filterIds) {
        if (field.endsWith("-client") && client != null) {
            return switch (field) {
                case "id-client" -> client.getId() != null ? String.valueOf(client.getId()) : "";
                case "company-client" -> client.getCompany() != null ? client.getCompany() : "";
                case "person-client" -> client.getPerson() != null ? client.getPerson() : "";
                case "phoneNumbers-client" -> client.getPhoneNumbers() != null
                        ? String.join(", ", client.getPhoneNumbers()) : "";
                case "createdAt-client" -> client.getCreatedAt() != null ? client.getCreatedAt() : "";
                case "updatedAt-client" -> client.getUpdatedAt() != null ? client.getUpdatedAt() : "";
                case "status-client" -> client.getStatus() != null ? client.getStatus().getName() : "";
                case "source-client" -> client.getSource() != null ? client.getSource().getName() : "";
                case "location-client" -> client.getLocation() != null ? client.getLocation() : "";
                case "pricePurchase-client" -> client.getPricePurchase() != null ? client.getPricePurchase() : "";
                case "priceSale-client" -> client.getPriceSale() != null ? client.getPriceSale() : "";
                case "volumeMonth-client" -> client.getVolumeMonth() != null ? client.getVolumeMonth() : "";
                case "route-client" -> client.getRoute() != null ? client.getRoute().getName() : "";
                case "region-client" -> client.getRegion() != null ? client.getRegion().getName() : "";
                case "business-client" -> client.getBusiness() != null ? client.getBusiness().getName() : "";
                case "clientProduct-client" -> client.getClientProduct() != null ? client.getClientProduct().getName() : "";
                case "edrpou-client" -> client.getEdrpou() != null ? client.getEdrpou() : "";
                case "enterpriseName-client" -> client.getEnterpriseName() != null ? client.getEnterpriseName() : "";
                case "vat-client" -> Boolean.TRUE.equals(client.getVat()) ? "так" : "";
                case "comment-client" -> client.getComment() != null ? client.getComment() : "";
                default -> "";
            };
        } else {
            return switch (field) {
                case "id" -> sale.getId() != null ? String.valueOf(sale.getId()) : "";
                case "user" -> getNameFromDTOList(filterIds.userDTOs(), sale.getUser());
                case "source" -> getNameFromDTOList(filterIds.sourceDTOs(), sale.getSource());
                case "product" -> filterIds.productDTOs().stream()
                        .filter(product -> product.getId().equals(sale.getProduct()))
                        .findFirst()
                        .map(Product::getName)
                        .orElse("");
                case "quantity" -> sale.getQuantity() != null ? sale.getQuantity().toString() : "";
                case "unitPrice" -> sale.getUnitPrice() != null ? sale.getUnitPrice().toString() : "";
                case "totalPrice" -> sale.getTotalPrice() != null ? sale.getTotalPrice().toString() : "";
                case "paymentMethod" -> sale.getPaymentMethod() != null
                        ? (sale.getPaymentMethod() == PaymentMethod.CASH ? "2" : "1") : "";
                case "currency" -> sale.getCurrency() != null ? sale.getCurrency() : "";
                case "exchangeRate" -> sale.getExchangeRate() != null ? String.valueOf(sale.getExchangeRate()) : "";
                case "transaction" -> sale.getTransaction() != null ? sale.getTransaction().toString() : "";
                case "createdAt" -> sale.getCreatedAt() != null ? sale.getCreatedAt().toString() : "";
                case "updatedAt" -> sale.getUpdatedAt() != null ? sale.getUpdatedAt().toString() : "";
                default -> "";
            };
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
        response.setHeader("Content-Disposition", "attachment; filename=sale_data.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }


    @Override
    public SaleReportDTO generateReport(String query, Map<String, List<String>> filterParams) {
        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        FilterIds filterIds = fetchFilterIds(clients);

        Map<Long, ClientDTO> clientMap = clients.stream()
                .collect(Collectors.toMap(ClientDTO::getId, client -> client));

        List<Sale> saleList = fetchSales(query, filterParams, clients.stream().map(ClientDTO::getId).toList(),
                filterIds.sourceIds(), Sort.by("id"));

        BigDecimal totalCollected = saleList.stream()
                .map(Sale::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = saleList.stream()
                .map(Sale::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byDrivers = saleList.stream()
                .filter(sale -> sale.getUser() != null)
                .collect(Collectors.groupingBy(
                        sale -> getNameFromDTOList(filterIds.userDTOs(), sale.getUser()),
                        Collectors.reducing(BigDecimal.ZERO, Sale::getQuantity, BigDecimal::add)
                ));

        Map<String, BigDecimal> byAttractors = saleList.stream()
                .filter(sale -> sale.getSource() != null)
                .collect(Collectors.groupingBy(
                        sale -> getNameFromDTOList(filterIds.sourceDTOs(), sale.getSource()),
                        Collectors.reducing(BigDecimal.ZERO, Sale::getQuantity, BigDecimal::add)
                ));

        Map<String, BigDecimal> byRegions = saleList.stream()
                .filter(sale -> sale.getClient() != null && clientMap.containsKey(sale.getClient()))
                .filter(purchase -> clientMap.get(purchase.getClient()).getRegion() != null)
                .collect(Collectors.groupingBy(
                        sale -> clientMap.get(sale.getClient()).getRegion().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Sale::getQuantity, BigDecimal::add)
                ));

        BigDecimal averagePrice = totalCollected.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalCollected, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal averageCollectedPerTime = totalCollected.divide(
                new BigDecimal(saleList.size()), 2, RoundingMode.HALF_UP);

        return SaleReportDTO.builder()
                .totalCollected(totalCollected.doubleValue())
                .totalSpent(totalSpent.doubleValue())
                .byDrivers(byDrivers.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue())))
                .byAttractors(byAttractors.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue())))
                .byRegions(byRegions.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue())))
                .averagePrice(averagePrice.doubleValue())
                .averageCollectedPerTime(averageCollectedPerTime.doubleValue())
                .build();
    }
}
