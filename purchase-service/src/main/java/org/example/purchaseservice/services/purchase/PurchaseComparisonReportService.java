package org.example.purchaseservice.services.purchase;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.client.ClientSearchRequest;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IProductService;
import feign.FeignException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseComparisonReportService {
    
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String COMPARISON_EXCEL_FILENAME = "purchase_report.xlsx";
    private static final String ALL_PRODUCTS_FILTER = "all";
    private static final String UNKNOWN_PRODUCT = "Unknown Product";
    private static final Long DEFAULT_PRODUCT_ID = 1L;
    
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final IProductService productService;
    
    public record DateRange(LocalDateTime fromDateTime, LocalDateTime toDateTime) {}
    
    public void generateComparisonExcelFile(@NonNull String purchaseDataFrom, @NonNull String purchaseDataTo, 
                                            @NonNull HttpServletResponse response) {
        try {
            DateRange dateRange = parseAndValidateDateRange(purchaseDataFrom, purchaseDataTo);
            List<ClientDTO> clients = fetchAllClients();
            Map<Long, String> productNames = fetchProductNames();
            
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Purchase Report");
            
            createComparisonHeaderRow(sheet);
            
            Map<Long, Map<Long, BigDecimal>> clientProductSums = calculateClientProductSums(clients, dateRange);
            fillComparisonDataRows(sheet, clients, clientProductSums, productNames);
            
            autoSizeColumns(sheet);
            sendComparisonExcelResponse(workbook, response);

        } catch (DateTimeParseException e) {
            throw new PurchaseException("INVALID_DATE_FORMAT", 
                    String.format("Invalid date format. Please use %s", DATE_PATTERN));
        } catch (PurchaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating Excel file", e);
            throw new PurchaseException("EXCEL_GENERATION_ERROR", 
                String.format("Failed to generate Excel file: %s", e.getMessage()));
        }
    }

    private DateRange parseAndValidateDateRange(@NonNull String purchaseDataFrom, @NonNull String purchaseDataTo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        LocalDate fromDate = LocalDate.parse(purchaseDataFrom, formatter);
        LocalDate toDate = LocalDate.parse(purchaseDataTo, formatter);

        if (fromDate.isAfter(toDate)) {
            throw new PurchaseException("INVALID_DATE_RANGE", 
                    "Start date cannot be after end date");
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59, 999999999);
        return new DateRange(fromDateTime, toDateTime);
    }

    private Map<Long, String> fetchProductNames() {
        return productService.getAllProducts(ALL_PRODUCTS_FILTER).stream()
                .filter(product -> product != null && product.getId() != null && product.getName() != null)
                .collect(Collectors.toMap(Product::getId, Product::getName, (existing, _) -> existing));
    }

    private void createComparisonHeaderRow(@NonNull XSSFSheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Client ID", "Company", "Product", "Total Volume"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
    }

    private Map<Long, Map<Long, BigDecimal>> calculateClientProductSums(@NonNull List<ClientDTO> clients, 
                                                                        @NonNull DateRange dateRange) {
        List<Long> clientIds = clients.stream()
                .filter(client -> client != null && client.getId() != null)
                .map(ClientDTO::getId)
                .toList();

        Specification<Purchase> spec = buildPurchaseSpecification(clientIds, dateRange);
        List<Purchase> allPurchases = purchaseRepository.findAll(spec, Pageable.unpaged()).getContent();

        return allPurchases.stream()
                .filter(p -> p.getClient() != null && p.getProduct() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        Purchase::getClient,
                        Collectors.groupingBy(
                                Purchase::getProduct,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        Purchase::getQuantity,
                                        BigDecimal::add
                                )
                        )
                ));
    }

    private Specification<Purchase> buildPurchaseSpecification(@NonNull List<Long> clientIds, @NonNull DateRange dateRange) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!clientIds.isEmpty()) {
                predicates.add(root.get("client").in(clientIds));
            }
            predicates.add(cb.between(root.get("createdAt"), dateRange.fromDateTime(), dateRange.toDateTime()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void fillComparisonDataRows(@NonNull XSSFSheet sheet, @NonNull List<ClientDTO> clients,
                                       @NonNull Map<Long, Map<Long, BigDecimal>> clientProductSums,
                                       @NonNull Map<Long, String> productNames) {
        int rowNum = 1;
        for (ClientDTO client : clients) {
            if (client == null || client.getId() == null) {
                continue;
            }
            
            Map<Long, BigDecimal> productSums = clientProductSums.getOrDefault(client.getId(), Collections.emptyMap());
            
            if (productSums.isEmpty()) {
                createComparisonRow(sheet, rowNum++, client, DEFAULT_PRODUCT_ID, BigDecimal.ZERO, productNames);
            } else {
                for (Map.Entry<Long, BigDecimal> entry : productSums.entrySet()) {
                    createComparisonRow(sheet, rowNum++, client, entry.getKey(), entry.getValue(), productNames);
                }
            }
        }
    }

    private void createComparisonRow(@NonNull XSSFSheet sheet, int rowNum, @NonNull ClientDTO client,
                                    Long productId, @NonNull BigDecimal quantity, @NonNull Map<Long, String> productNames) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(client.getId());
        row.createCell(1).setCellValue(client.getCompany() != null ? client.getCompany() : "");
        row.createCell(2).setCellValue(productNames.getOrDefault(productId, UNKNOWN_PRODUCT));
        row.createCell(3).setCellValue(quantity.doubleValue());
    }

    private void autoSizeColumns(@NonNull XSSFSheet sheet) {
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void sendComparisonExcelResponse(@NonNull XSSFWorkbook workbook, @NonNull HttpServletResponse response) throws IOException {
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=" + COMPARISON_EXCEL_FILENAME);
        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    private List<ClientDTO> fetchAllClients() {
        try {
            ClientSearchRequest request = new ClientSearchRequest(null, null, null);
            List<ClientDTO> clients = clientApiClient.searchClients(request).getBody();
            return clients != null ? clients : Collections.emptyList();
        } catch (FeignException e) {
            log.error("Feign error fetching all clients: status={}, error={}", 
                    e.status(), e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error fetching all clients: error={}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
