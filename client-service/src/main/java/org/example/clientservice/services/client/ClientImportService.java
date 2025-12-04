package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.clienttype.ClientTypeFieldListValue;
import org.example.clientservice.models.clienttype.FieldType;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.IClientCrudService;
import org.example.clientservice.services.impl.IClientImportService;
import org.example.clientservice.services.clienttype.ClientTypeFieldService;
import org.example.clientservice.services.clienttype.ClientTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientImportService implements IClientImportService {
    private final ClientTypeService clientTypeService;
    private final ClientTypeFieldService clientTypeFieldService;
    private final ClientRepository clientRepository;
    private final IClientCrudService clientCrudService;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    @Override
    public byte[] generateTemplate(Long clientTypeId) {
        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
        List<ClientTypeField> fields = clientTypeFieldService.getFieldsByClientTypeId(clientTypeId);
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Clients");
        
        // Создаем заголовки
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        
        // Статические поля
        headerRow.createCell(colIndex++).setCellValue("ID (опціонально)");
        headerRow.createCell(colIndex++).setCellValue(clientType.getNameFieldLabel() != null ? clientType.getNameFieldLabel() : "Компанія");
        headerRow.createCell(colIndex++).setCellValue("Залучення (ID)");
        headerRow.createCell(colIndex++).setCellValue("Дата створення (yyyy-MM-dd або yyyy-MM-dd HH:mm:ss)");
        headerRow.createCell(colIndex++).setCellValue("Дата оновлення (yyyy-MM-dd або yyyy-MM-dd HH:mm:ss)");
        headerRow.createCell(colIndex++).setCellValue("Активний (Так/Ні)");
        
        // Динамические поля
        for (ClientTypeField field : fields) {
            String header = field.getFieldLabel();
            if (field.getAllowMultiple() != null && field.getAllowMultiple()) {
                header += " (через кому, якщо кілька)";
            }
            headerRow.createCell(colIndex++).setCellValue(header);
        }
        
        // Пример строки
        Row exampleRow = sheet.createRow(1);
        colIndex = 0;
        exampleRow.createCell(colIndex++).setCellValue(""); // ID
        exampleRow.createCell(colIndex++).setCellValue("Приклад компанії"); // Company
        exampleRow.createCell(colIndex++).setCellValue(""); // Source
        exampleRow.createCell(colIndex++).setCellValue(""); // CreatedAt
        exampleRow.createCell(colIndex++).setCellValue(""); // UpdatedAt
        exampleRow.createCell(colIndex++).setCellValue("Так"); // isActive
        
        for (ClientTypeField field : fields) {
            Cell cell = exampleRow.createCell(colIndex++);
            switch (field.getFieldType()) {
                case TEXT -> cell.setCellValue("Приклад тексту");
                case NUMBER -> cell.setCellValue("123.45");
                case DATE -> cell.setCellValue("2024-01-01");
                case PHONE -> cell.setCellValue("+380123456789");
                case BOOLEAN -> cell.setCellValue("Так");
                case LIST -> {
                    if (field.getListValues() != null && !field.getListValues().isEmpty()) {
                        String exampleValue = field.getListValues().get(0).getValue();
                        if (field.getAllowMultiple() != null && field.getAllowMultiple() && field.getListValues().size() > 1) {
                            exampleValue += ", " + field.getListValues().get(1).getValue();
                        }
                        cell.setCellValue(exampleValue);
                    }
                }
            }
        }
        
        // Автоматическая ширина колонок
        for (int i = 0; i < colIndex; i++) {
            sheet.autoSizeColumn(i);
        }
        
        return convertWorkbookToBytes(workbook);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importClients(Long clientTypeId, MultipartFile file) {
        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
        List<ClientTypeField> fields = clientTypeFieldService.getFieldsByClientTypeId(clientTypeId);
        
        // Загружаем listValues для полей типа LIST
        for (ClientTypeField field : fields) {
            if (field.getFieldType() == FieldType.LIST && field.getListValues() != null) {
                field.getListValues().size(); // Инициализируем коллекцию
            }
        }
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet.getPhysicalNumberOfRows() < 2) {
                throw new ClientException("Excel файл повинен містити принаймні заголовок та один рядок даних");
            }
            
            // Парсим заголовки
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnIndexMap = parseHeaders(headerRow, fields);
            
            // Валидируем и создаем клиентов
            List<Client> clientsToCreate = new ArrayList<>();
            StringBuilder errors = new StringBuilder();
            
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                
                try {
                    Client client = parseClientRow(row, clientType, fields, columnIndexMap, rowIndex + 1);
                    clientsToCreate.add(client);
                } catch (Exception e) {
                    errors.append("Рядок ").append(rowIndex + 1).append(": ").append(e.getMessage()).append("\n");
                }
            }
            
            if (!errors.isEmpty()) {
                throw new ClientException("Помилки при імпорті:\n" + errors.toString());
            }
            
            // Сохраняем всех клиентов
            for (Client client : clientsToCreate) {
                clientCrudService.createClient(client);
            }
            
            return "Успішно імпортовано " + clientsToCreate.size() + " клієнтів";
            
        } catch (IOException e) {
            throw new ClientException("Помилка читання Excel файлу: " + e.getMessage());
        }
    }
    
    private Map<String, Integer> parseHeaders(Row headerRow, List<ClientTypeField> fields) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellValueAsString(cell).trim();
                
                if (headerValue.contains("ID")) {
                    columnIndexMap.put("id", i);
                } else if (headerValue.contains("Компанія") || headerValue.contains("Назва")) {
                    columnIndexMap.put("company", i);
                } else if (headerValue.contains("Залучення")) {
                    columnIndexMap.put("source", i);
                } else if (headerValue.contains("створення") || headerValue.contains("Created")) {
                    columnIndexMap.put("createdAt", i);
                } else if (headerValue.contains("оновлення") || headerValue.contains("Updated")) {
                    columnIndexMap.put("updatedAt", i);
                } else if (headerValue.contains("Активний") || headerValue.contains("Active")) {
                    columnIndexMap.put("isActive", i);
                } else {
                    // Ищем по fieldLabel
                    for (ClientTypeField field : fields) {
                        if (headerValue.startsWith(field.getFieldLabel())) {
                            columnIndexMap.put("field_" + field.getId(), i);
                            break;
                        }
                    }
                }
            }
        }
        
        return columnIndexMap;
    }
    
    private Client parseClientRow(Row row, ClientType clientType, List<ClientTypeField> fields, 
                                  Map<String, Integer> columnIndexMap, int rowNumber) {
        Client client = new Client();
        client.setClientType(clientType);
        client.setFieldValues(new ArrayList<>());
        
        // ID
        Integer idCol = columnIndexMap.get("id");
        if (idCol != null) {
            Cell idCell = row.getCell(idCol);
            if (idCell != null && !getCellValueAsString(idCell).trim().isEmpty()) {
                try {
                    Long id = Long.parseLong(getCellValueAsString(idCell).trim());
                    if (clientRepository.existsById(id)) {
                        throw new ClientException("ID " + id + " вже зайнятий");
                    }
                    client.setId(id);
                } catch (NumberFormatException e) {
                    throw new ClientException("Невірний формат ID");
                }
            }
        }
        
        // Company (обязательное поле)
        Integer companyCol = columnIndexMap.get("company");
        if (companyCol == null) {
            throw new ClientException("Не знайдено колонку з назвою компанії");
        }
        Cell companyCell = row.getCell(companyCol);
        String company = getCellValueAsString(companyCell);
        if (company == null || company.trim().isEmpty()) {
            throw new ClientException("Назва компанії є обов'язковим полем");
        }
        client.setCompany(company.trim());
        
        // Source
        Integer sourceCol = columnIndexMap.get("source");
        if (sourceCol != null) {
            Cell sourceCell = row.getCell(sourceCol);
            String sourceValue = getCellValueAsString(sourceCell);
            if (sourceValue != null && !sourceValue.trim().isEmpty()) {
                try {
                    Long sourceId = Long.parseLong(sourceValue.trim());
                    client.setSource(sourceId);
                } catch (NumberFormatException e) {
                    throw new ClientException("Невірний формат ID залучення");
                }
            }
        }
        
        // CreatedAt
        Integer createdAtCol = columnIndexMap.get("createdAt");
        if (createdAtCol != null) {
            Cell createdAtCell = row.getCell(createdAtCol);
            String createdAtValue = getCellValueAsString(createdAtCell);
            if (createdAtValue != null && !createdAtValue.trim().isEmpty()) {
                LocalDateTime createdAt = parseDateTime(createdAtValue.trim());
                client.setCreatedAt(createdAt);
            }
        }
        
        // UpdatedAt
        Integer updatedAtCol = columnIndexMap.get("updatedAt");
        if (updatedAtCol != null) {
            Cell updatedAtCell = row.getCell(updatedAtCol);
            String updatedAtValue = getCellValueAsString(updatedAtCell);
            if (updatedAtValue != null && !updatedAtValue.trim().isEmpty()) {
                LocalDateTime updatedAt = parseDateTime(updatedAtValue.trim());
                client.setUpdatedAt(updatedAt);
            }
        }
        
        // isActive (по умолчанию true)
        Integer isActiveCol = columnIndexMap.get("isActive");
        if (isActiveCol != null) {
            Cell isActiveCell = row.getCell(isActiveCol);
            String isActiveValue = getCellValueAsString(isActiveCell);
            if (isActiveValue != null && !isActiveValue.trim().isEmpty()) {
                client.setIsActive(parseBoolean(isActiveValue.trim()));
            } else {
                client.setIsActive(true);
            }
        } else {
            client.setIsActive(true);
        }
        
        // Динамические поля
        for (ClientTypeField field : fields) {
            Integer fieldCol = columnIndexMap.get("field_" + field.getId());
            if (fieldCol != null) {
                Cell fieldCell = row.getCell(fieldCol);
                String fieldValue = getCellValueAsString(fieldCell);
                
                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    List<ClientFieldValue> fieldValues = parseFieldValue(client, field, fieldValue.trim(), rowNumber);
                    client.getFieldValues().addAll(fieldValues);
                } else if (field.getIsRequired() != null && field.getIsRequired()) {
                    throw new ClientException("Поле '" + field.getFieldLabel() + "' є обов'язковим");
                }
            } else if (field.getIsRequired() != null && field.getIsRequired()) {
                throw new ClientException("Поле '" + field.getFieldLabel() + "' є обов'язковим");
            }
        }
        
        return client;
    }
    
    private List<ClientFieldValue> parseFieldValue(Client client, ClientTypeField field, String value, int rowNumber) {
        List<ClientFieldValue> fieldValues = new ArrayList<>();
        
        // Если поле поддерживает множественные значения и значение содержит запятую
        if (field.getAllowMultiple() != null && field.getAllowMultiple() && value.contains(",")) {
            String[] values = value.split(",");
            for (int i = 0; i < values.length; i++) {
                String singleValue = values[i].trim();
                if (!singleValue.isEmpty()) {
                    ClientFieldValue fieldValue = createFieldValue(client, field, singleValue, i, rowNumber);
                    fieldValues.add(fieldValue);
                }
            }
        } else {
            ClientFieldValue fieldValue = createFieldValue(client, field, value, 0, rowNumber);
            fieldValues.add(fieldValue);
        }
        
        return fieldValues;
    }
    
    private ClientFieldValue createFieldValue(Client client, ClientTypeField field, String value, int displayOrder, int rowNumber) {
        ClientFieldValue fieldValue = new ClientFieldValue();
        fieldValue.setClient(client);
        fieldValue.setField(field);
        fieldValue.setDisplayOrder(displayOrder);
        
        switch (field.getFieldType()) {
            case TEXT, PHONE -> {
                fieldValue.setValueText(value);
            }
            case NUMBER -> {
                try {
                    BigDecimal numberValue = new BigDecimal(value);
                    fieldValue.setValueNumber(numberValue);
                } catch (NumberFormatException e) {
                    throw new ClientException("Поле '" + field.getFieldLabel() + "': невірний формат числа: " + value);
                }
            }
            case DATE -> {
                try {
                    LocalDate dateValue = LocalDate.parse(value, DATE_FORMATTER);
                    fieldValue.setValueDate(dateValue);
                } catch (DateTimeParseException e) {
                    throw new ClientException("Поле '" + field.getFieldLabel() + "': невірний формат дати (очікується yyyy-MM-dd): " + value);
                }
            }
            case BOOLEAN -> {
                fieldValue.setValueBoolean(parseBoolean(value));
            }
            case LIST -> {
                // Ищем значение в списке
                ClientTypeFieldListValue listValue = findListValue(field, value);
                if (listValue == null) {
                    throw new ClientException("Поле '" + field.getFieldLabel() + "': значення '" + value + "' не знайдено в списку доступних значень");
                }
                fieldValue.setValueList(listValue);
            }
        }
        
        return fieldValue;
    }
    
    private ClientTypeFieldListValue findListValue(ClientTypeField field, String value) {
        if (field.getListValues() == null) {
            return null;
        }
        
        return field.getListValues().stream()
                .filter(lv -> lv.getValue().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(null);
    }
    
    private Boolean parseBoolean(String value) {
        String trimmed = value.trim();
        if ("Так".equalsIgnoreCase(trimmed) || "true".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
            return true;
        } else if ("Ні".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
            return false;
        } else {
            throw new ClientException("Невірне значення boolean. Очікується 'Так' або 'Ні'");
        }
    }
    
    private LocalDateTime parseDateTime(String value) {
        // Пробуем сначала с временем
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Если не получилось, пробуем только дату
            try {
                LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
                return date.atTime(LocalTime.MIN);
            } catch (DateTimeParseException ex) {
                throw new ClientException("Невірний формат дати/часу. Очікується yyyy-MM-dd або yyyy-MM-dd HH:mm:ss");
            }
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(DATE_TIME_FORMATTER);
                } else {
                    // Убираем лишние нули после запятой для чисел
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private byte[] convertWorkbookToBytes(Workbook workbook) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ClientException("Помилка генерації Excel файлу: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                log.warn("Failed to close workbook: {}", e.getMessage());
            }
        }
    }
}

