package org.example.purchaseservice.services.purchase;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.clients.*;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.warehouse.WarehouseReceipt;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.models.dto.fields.*;
import org.example.purchaseservice.models.dto.impl.IdNameDTO;
import org.example.purchaseservice.models.dto.purchase.PurchaseReportDTO;
import org.example.purchaseservice.models.dto.user.UserDTO;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IProductService;
import org.example.purchaseservice.services.impl.IPurchaseSpecialOperationsService;
import org.example.purchaseservice.services.impl.IWarehouseReceiptService;
import org.example.purchaseservice.spec.PurchaseSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSpecialOperationsService implements IPurchaseSpecialOperationsService {

    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final UserClient userClient;
    private final IProductService productService;
    private final IWarehouseReceiptService warehouseReceiptService;

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
        FilterIds filterIds = createFilterIds(clients);

        if (clients.isEmpty()) {
            log.info("No clients found for the given filters, returning empty workbook");
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        List<Purchase> purchaseList = fetchPurchases(
                query, filterParams, clients.stream().map(ClientDTO::getId).toList(), filterIds.sourceIds(), sort);
        Map<Long, ClientDTO> clientMap = fetchClientMap(clients);

        Workbook workbook = generateWorkbook(purchaseList, selectedFields, filterIds, clientMap);
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

    private FilterIds createFilterIds(List<ClientDTO> clients) {

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
                            key.equals("route") || key.equals("region") || key.equals("source-client") ||
                            key.equals("clientProduct");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        ClientSearchRequest clientRequest = new ClientSearchRequest(query, filteredParams);
        return clientApiClient.searchClients(clientRequest);
    }

    private Map<Long, ClientDTO> fetchClientMap(List<ClientDTO> clients) {
        return clients.stream().collect(Collectors.toMap(ClientDTO::getId, client -> client));
    }

    private List<Purchase> fetchPurchases(String query, Map<String, List<String>> filterParams, List<Long> clientIds,
                                          List<Long> sourceIds, Sort sort) {
        Specification<Purchase> spec = (root, querySpec, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            } else {
                return criteriaBuilder.disjunction();
            }

            Specification<Purchase> purchaseSpec = new PurchaseSpecification(query, filterParams, clientIds, sourceIds);
            predicates.add(purchaseSpec.toPredicate(root, querySpec, criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return purchaseRepository.findAll(spec, sort);
    }

    private Workbook generateWorkbook(List<Purchase> purchaseList, List<String> selectedFields, FilterIds filterIds,
                                      Map<Long, ClientDTO> clientMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap();
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, purchaseList, selectedFields, filterIds, clientMap);

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

    private void fillDataRows(Sheet sheet, List<Purchase> purchaseList, List<String> selectedFields, FilterIds filterIds,
                              Map<Long, ClientDTO> clientMap) {
        int rowIndex = 1;
        for (Purchase purchase : purchaseList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(purchase.getClient());
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(getFieldValue(purchase, client, field, filterIds));
            }
        }
    }

    private String getFieldValue(Purchase purchase, ClientDTO client, String field, FilterIds filterIds) {
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
                case "idHEX0x20id" -> purchase.getId() != null ? String.valueOf(purchase.getId()) : "";
                case "user" -> getNameFromDTOList(filterIds.userDTOs(), purchase.getUser());
                case "source" -> getNameFromDTOList(filterIds.sourceDTOs(), purchase.getSource());
                case "product" -> filterIds.productDTOs().stream()
                        .filter(product -> product.getId().equals(purchase.getProduct()))
                        .findFirst()
                        .map(Product::getName)
                        .orElse("");
                case "quantity" -> purchase.getQuantity() != null ? purchase.getQuantity().toString() : "";
                case "unitPrice" -> purchase.getUnitPrice() != null ? purchase.getUnitPrice().toString() : "";
                case "totalPrice" -> purchase.getTotalPrice() != null ? purchase.getTotalPrice().toString() : "";
                case "paymentMethod" -> purchase.getPaymentMethod() != null
                        ? (purchase.getPaymentMethod() == PaymentMethod.CASH ? "2" : "1") : "";
                case "currency" -> purchase.getCurrency() != null ? purchase.getCurrency() : "";
                case "exchangeRate" -> purchase.getExchangeRate() != null ? String.valueOf(purchase.getExchangeRate()) : "";
                case "transaction" -> purchase.getTransaction() != null ? purchase.getTransaction().toString() : "";
                case "createdAt" -> purchase.getCreatedAt() != null ? purchase.getCreatedAt().toString() : "";
                case "updatedAt" -> purchase.getUpdatedAt() != null ? purchase.getUpdatedAt().toString() : "";
                case "comment" -> purchase.getComment() != null ? purchase.getComment() : "";
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
        response.setHeader("Content-Disposition", "attachment; filename=purchase_data.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Override
    public PurchaseReportDTO generateReport(String query, Map<String, List<String>> filterParams) {

        List<ClientDTO> clients = fetchClientIds(query, filterParams);
        FilterIds filterIds = createFilterIds(clients);
        List<Purchase> purchaseList = fetchPurchases(query, filterParams, clients, filterIds);
        List<WarehouseReceipt> warehouseReceipts = fetchWarehouseReceipts(filterParams);

        Map<Long, Double> totalCollectedByProduct = calculateTotalCollectedByProduct(purchaseList);
        Map<Long, Double> totalDeliveredByProduct = calculateTotalDeliveredByProduct(warehouseReceipts);
        Map<String, Map<Long, Double>> byDrivers = calculateByDrivers(purchaseList, filterIds);
        Map<String, Map<Long, Double>> byAttractors = calculateByAttractors(purchaseList, filterIds);
        Map<String, Double> totalSpentByCurrency = calculateTotalSpentByCurrency(purchaseList);
        Map<String, Double> averagePriceByCurrency = calculateAveragePriceByCurrency(purchaseList);
        Map<Long, Double> averageCollectedPerTimeByProduct = calculateAverageCollectedPerTimeByProduct(purchaseList);

        return buildReport(
                totalCollectedByProduct,
                totalDeliveredByProduct,
                byDrivers,
                byAttractors,
                totalSpentByCurrency,
                averagePriceByCurrency,
                averageCollectedPerTimeByProduct
        );
    }

    private List<Purchase> fetchPurchases(String query, Map<String, List<String>> filterParams,
                                          List<ClientDTO> clients, FilterIds filterIds) {
        return fetchPurchases(
                query,
                filterParams,
                clients.stream().map(ClientDTO::getId).toList(),
                filterIds.sourceIds(),
                Sort.by("id")
        );
    }

    private List<WarehouseReceipt> fetchWarehouseReceipts(Map<String, List<String>> filterParams) {
        Map<String, List<String>> warehouseFilters = new HashMap<>();
        Map<String, String> filterKeyMapping = new HashMap<>();
        filterKeyMapping.put("user", "user_id");
        filterKeyMapping.put("product", "product_id");
        filterKeyMapping.put("createdAtFrom", "entry_date_from");
        filterKeyMapping.put("createdAtTo", "entry_date_to");

        for (Map.Entry<String, String> entry : filterKeyMapping.entrySet()) {
            String frontendKey = entry.getKey();
            String warehouseKey = entry.getValue();
            if (filterParams.containsKey(frontendKey)) {
                warehouseFilters.put(warehouseKey, filterParams.get(frontendKey));
            }
        }

        return warehouseReceiptService.findWarehouseReceiptsByFilters(warehouseFilters);
    }

    private Map<Long, Double> calculateTotalCollectedByProduct(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getProduct,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Purchase::getQuantity,
                                BigDecimal::add
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    private Map<Long, Double> calculateTotalDeliveredByProduct(List<WarehouseReceipt> warehouseReceipts) {
        return warehouseReceipts.stream()
                .filter(e -> e.getProductId() != null && e.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        WarehouseReceipt::getProductId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                WarehouseReceipt::getQuantity,
                                BigDecimal::add
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    private Map<String, Map<Long, Double>> calculateByDrivers(List<Purchase> purchaseList, FilterIds filterIds) {
        return purchaseList.stream()
                .filter(p -> p.getUser() != null && p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        purchase -> getNameFromDTOList(filterIds.userDTOs(), purchase.getUser()),
                        Collectors.groupingBy(
                                Purchase::getProduct,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Purchase::getQuantity,
                                        BigDecimal::add
                                )
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        v -> v.getValue().doubleValue()
                                ))
                ));
    }

    private Map<String, Map<Long, Double>> calculateByAttractors(List<Purchase> purchaseList, FilterIds filterIds) {
        return purchaseList.stream()
                .filter(p -> p.getSource() != null && p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        purchase -> getNameFromDTOList(filterIds.sourceDTOs(), purchase.getSource()),
                        Collectors.groupingBy(
                                Purchase::getProduct,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Purchase::getQuantity,
                                        BigDecimal::add
                                )
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        v -> v.getValue().doubleValue()
                                ))
                ));
    }

    private Map<String, Double> calculateTotalSpentByCurrency(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getCurrency() != null && p.getTotalPrice() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getCurrency,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Purchase::getTotalPrice,
                                BigDecimal::add
                        )
                )).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().doubleValue()
                ));
    }

    private Map<String, Double> calculateAveragePriceByCurrency(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getCurrency() != null && p.getTotalPrice() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getCurrency,
                        Collectors.collectingAndThen(
                                Collectors.averagingDouble(p ->
                                        p.getQuantity().compareTo(BigDecimal.ZERO) > 0
                                                ? p.getTotalPrice().divide(p.getQuantity(), 6, RoundingMode.HALF_UP).doubleValue()
                                                : 0.0
                                ),
                                avg -> avg
                        )
                ));
    }

    private Map<Long, Double> calculateAverageCollectedPerTimeByProduct(List<Purchase> purchaseList) {
        return purchaseList.stream()
                .filter(p -> p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getProduct,
                        Collectors.collectingAndThen(
                                Collectors.averagingDouble(p -> p.getQuantity().doubleValue()),
                                avg -> avg
                        )
                ));
    }

    private PurchaseReportDTO buildReport(
            Map<Long, Double> totalCollectedByProduct,
            Map<Long, Double> totalDeliveredByProduct,
            Map<String, Map<Long, Double>> byDrivers,
            Map<String, Map<Long, Double>> byAttractors,
            Map<String, Double> totalSpentByCurrency,
            Map<String, Double> averagePriceByCurrency,
            Map<Long, Double> averageCollectedPerTimeByProduct
    ) {
        return PurchaseReportDTO.builder()
                .totalCollectedByProduct(totalCollectedByProduct)
                .totalDeliveredByProduct(totalDeliveredByProduct)
                .byDrivers(byDrivers)
                .byAttractors(byAttractors)
                .totalSpentByCurrency(totalSpentByCurrency)
                .averagePriceByCurrency(averagePriceByCurrency)
                .averageCollectedPerTimeByProduct(averageCollectedPerTimeByProduct)
                .build();
    }


    @Override
    public void generateComparisonExcelFile(String purchaseDataFrom, String purchaseDataTo, HttpServletResponse response) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate fromDate = LocalDate.parse(purchaseDataFrom, formatter);
            LocalDate toDate = LocalDate.parse(purchaseDataTo, formatter);

            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            LocalDateTime toDateTime = toDate.atTime(23, 59, 59, 999999999);

            ClientSearchRequest request = new ClientSearchRequest(null, null);
            List<ClientDTO> clients = clientApiClient.searchClients(request);

            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Purchase Report");

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Client ID", "Company", "Phone Numbers", "Location", "Region", "Product", "Total Volume"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;

            Map<Long, String> productNames = productService.getAllProducts("all").stream()
                    .collect(Collectors.toMap(Product::getId, Product::getName));

            for (ClientDTO client : clients) {
                Specification<Purchase> spec = (root, query, cb) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(cb.equal(root.get("client"), client.getId()));
                    predicates.add(cb.between(root.get("createdAt"), fromDateTime, toDateTime));
                    return cb.and(predicates.toArray(new Predicate[0]));
                };

                List<Purchase> purchases = purchaseRepository.findAll(spec, Pageable.unpaged()).getContent();

                Map<Long, BigDecimal> productSums = purchases.stream()
                        .collect(Collectors.groupingBy(
                                Purchase::getProduct,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Purchase::getQuantity,
                                        BigDecimal::add
                                )
                        ));
                if (productSums.isEmpty()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(client.getId());
                    row.createCell(1).setCellValue(client.getCompany());
                    row.createCell(2).setCellValue(String.join(", ", client.getPhoneNumbers()));
                    row.createCell(3).setCellValue(client.getLocation());
                    row.createCell(4).setCellValue(client.getRegion() != null ? client.getRegion().getName() : "");
                    row.createCell(5).setCellValue(productNames.getOrDefault(1L, "Unknown Product"));
                    row.createCell(6).setCellValue(0);
                } else {
                    for (Map.Entry<Long, BigDecimal> entry : productSums.entrySet()) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(client.getId());
                        row.createCell(1).setCellValue(client.getCompany());
                        row.createCell(2).setCellValue(String.join(", ", client.getPhoneNumbers()));
                        row.createCell(3).setCellValue(client.getLocation());
                        row.createCell(4).setCellValue(client.getRegion() != null ? client.getRegion().getName() : "");
                        row.createCell(5).setCellValue(productNames.getOrDefault(entry.getKey(), "Unknown Product"));
                        row.createCell(6).setCellValue(entry.getValue().doubleValue());
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=purchase_report.xlsx");

            workbook.write(response.getOutputStream());
            workbook.close();

        } catch (DateTimeParseException e) {
            log.error("Invalid date format for purchaseDataFrom or purchaseDataTo. Expected format: yyyy-MM-dd", e);
            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd", e);
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }
}