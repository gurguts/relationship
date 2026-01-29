package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.purchaseservice.services.purchase.PurchaseExportDataFetcher.FilterIds;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseExcelGenerator {
    
    private static final String FIELD_PREFIX = "field_";
    private static final String CLIENT_SUFFIX = "-client";
    private static final String HEADER_CLIENT_SUFFIX = " (клієнта)";
    
    private static final String HEADER_ID_CLIENT = "Id (клієнта)";
    private static final String HEADER_COMPANY_CLIENT = "Компанія (клієнта)";
    private static final String HEADER_CREATED_AT_CLIENT = "Дата створення (клієнта)";
    private static final String HEADER_UPDATED_AT_CLIENT = "Дата оновлення (клієнта)";
    private static final String HEADER_SOURCE_CLIENT = "Залучення (клієнта)";
    private static final String HEADER_ID = "Id";
    private static final String HEADER_USER = "Водій";
    private static final String HEADER_SOURCE = "Залучення";
    private static final String HEADER_PRODUCT = "Товар";
    private static final String HEADER_QUANTITY = "Кількість";
    private static final String HEADER_UNIT_PRICE = "Ціна за од";
    private static final String HEADER_TOTAL_PRICE = "Повна ціна";
    private static final String HEADER_TOTAL_PRICE_EUR = "Всього сплачено EUR";
    private static final String HEADER_PAYMENT_METHOD = "Метод оплати";
    private static final String HEADER_CURRENCY = "Валюта";
    private static final String HEADER_EXCHANGE_RATE = "Курс";
    private static final String HEADER_TRANSACTION = "Id транзакції";
    private static final String HEADER_CREATED_AT = "Дата створення";
    private static final String HEADER_UPDATED_AT = "Дата оновлення";
    private static final String HEADER_COMMENT = "Коментар";
    
    private final PurchaseExportDataFetcher dataFetcher;
    private final PurchaseFieldValueFormatter fieldValueFormatter;
    
    public Workbook generateWorkbook(List<Purchase> purchaseList, List<String> selectedFields, FilterIds filterIds,
                                      Map<Long, ClientDTO> clientMap, 
                                      Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Data");

        List<String> sortedFields = sortFields(selectedFields);
        Map<String, String> fieldToHeader = createFieldToHeaderMap(sortedFields);
        createHeaderRow(sheet, sortedFields, fieldToHeader);
        fillDataRows(sheet, purchaseList, sortedFields, filterIds, clientMap, clientFieldValuesMap);

        return workbook;
    }
    
    private List<String> sortFields(List<String> selectedFields) {
        List<String> clientFields = new ArrayList<>();
        List<String> purchaseFields = new ArrayList<>();
        
        for (String field : selectedFields) {
            if (field.endsWith(CLIENT_SUFFIX) || field.startsWith(FIELD_PREFIX)) {
                clientFields.add(field);
            } else {
                purchaseFields.add(field);
            }
        }
        
        List<String> sorted = new ArrayList<>(clientFields);
        sorted.addAll(purchaseFields);
        return sorted;
    }

    private Map<String, String> createFieldToHeaderMap(@NonNull List<String> selectedFields) {
        Map<String, String> headerMap = new HashMap<>();
        
        headerMap.put("id-client", HEADER_ID_CLIENT);
        headerMap.put("company-client", HEADER_COMPANY_CLIENT);
        headerMap.put("createdAt-client", HEADER_CREATED_AT_CLIENT);
        headerMap.put("updatedAt-client", HEADER_UPDATED_AT_CLIENT);
        headerMap.put("source-client", HEADER_SOURCE_CLIENT);
        
        headerMap.put("id", HEADER_ID);
        headerMap.put("user", HEADER_USER);
        headerMap.put("source", HEADER_SOURCE);
        headerMap.put("product", HEADER_PRODUCT);
        headerMap.put("quantity", HEADER_QUANTITY);
        headerMap.put("unitPrice", HEADER_UNIT_PRICE);
        headerMap.put("totalPrice", HEADER_TOTAL_PRICE);
        headerMap.put("totalPriceEur", HEADER_TOTAL_PRICE_EUR);
        headerMap.put("paymentMethod", HEADER_PAYMENT_METHOD);
        headerMap.put("currency", HEADER_CURRENCY);
        headerMap.put("exchangeRate", HEADER_EXCHANGE_RATE);
        headerMap.put("transaction", HEADER_TRANSACTION);
        headerMap.put("createdAt", HEADER_CREATED_AT);
        headerMap.put("updatedAt", HEADER_UPDATED_AT);
        headerMap.put("comment", HEADER_COMMENT);
        
        List<Long> fieldIds = selectedFields.stream()
                .filter(field -> field.startsWith(FIELD_PREFIX))
                .map(fieldValueFormatter::parseFieldId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, ClientTypeFieldDTO> fieldMap = dataFetcher.fetchClientTypeFields(fieldIds);
        
        for (String field : selectedFields) {
            if (field.startsWith(FIELD_PREFIX)) {
                Long fieldId = fieldValueFormatter.parseFieldId(field);
                if (fieldId != null) {
                    ClientTypeFieldDTO fieldDTO = fieldMap.get(fieldId);
                    String header = getHeader(field, fieldDTO);
                    headerMap.put(field, header);
                } else {
                    headerMap.put(field, field + HEADER_CLIENT_SUFFIX);
                }
            }
        }
        
        return headerMap;
    }

    @NotNull
    private static String getHeader(String field, ClientTypeFieldDTO fieldDTO) {
        String header;
        if (fieldDTO != null) {
            String label = fieldDTO.getFieldLabel();
            if (label != null && !label.trim().isEmpty()) {
                header = label + HEADER_CLIENT_SUFFIX;
            } else {
                String name = fieldDTO.getFieldName();
                header = (name != null && !name.trim().isEmpty())
                        ? name + HEADER_CLIENT_SUFFIX
                        : field + HEADER_CLIENT_SUFFIX;
            }
        } else {
            header = field + HEADER_CLIENT_SUFFIX;
        }
        return header;
    }

    private void createHeaderRow(Sheet sheet, List<String> selectedFields, Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            String header = fieldToHeader.getOrDefault(field, field);
            headerRow.createCell(colIndex++).setCellValue(header);
        }
    }

    private void fillDataRows(Sheet sheet, List<Purchase> purchaseList, List<String> selectedFields, FilterIds filterIds,
                              Map<Long, ClientDTO> clientMap, Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap) {
        int rowIndex = 1;
        for (Purchase purchase : purchaseList) {
            Row row = sheet.createRow(rowIndex++);
            int colIndex = 0;
            ClientDTO client = clientMap.get(purchase.getClient());
            List<ClientFieldValueDTO> fieldValues = client != null ? clientFieldValuesMap.getOrDefault(client.getId(), Collections.emptyList()) : Collections.emptyList();
            for (String field : selectedFields) {
                row.createCell(colIndex++).setCellValue(
                        fieldValueFormatter.getFieldValue(purchase, client, field, filterIds, fieldValues));
            }
        }
    }
}
