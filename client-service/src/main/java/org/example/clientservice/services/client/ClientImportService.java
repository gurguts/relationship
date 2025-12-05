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
import org.example.clientservice.services.impl.ISourceService;
import org.example.clientservice.services.clienttype.ClientTypeFieldService;
import org.example.clientservice.services.clienttype.ClientTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientImportService implements IClientImportService {
    @PersistenceContext
    private EntityManager entityManager;
    
    private final ClientTypeService clientTypeService;
    private final ClientTypeFieldService clientTypeFieldService;
    private final ClientRepository clientRepository;
    private final IClientCrudService clientCrudService;
    private final ISourceService sourceService;
    
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
        headerRow.createCell(colIndex++).setCellValue("Залучення (назва)");
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
            Map<String, Integer> columnIndexMap = parseHeaders(headerRow, fields, clientType);
            
            // Валидируем и создаем клиентов
            List<Client> clientsToCreate = new ArrayList<>();
            StringBuilder errors = new StringBuilder();
            
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                
                // Проверяем, что строка не пустая (содержит хотя бы одну непустую ячейку)
                boolean isEmptyRow = true;
                for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            isEmptyRow = false;
                            break;
                        }
                    }
                }
                
                if (isEmptyRow) {
                    continue; // Пропускаем пустые строки
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
            Long maxSpecifiedId = null;
            for (Client client : clientsToCreate) {
                if (client.getId() != null) {
                    // Если ID указан, используем нативный INSERT для вставки с указанным ID
                    Long specifiedId = client.getId();
                    
                    // Отслеживаем максимальный указанный ID для обновления AUTO_INCREMENT
                    if (maxSpecifiedId == null || specifiedId > maxSpecifiedId) {
                        maxSpecifiedId = specifiedId;
                    }
                    
                    // Формируем нативный INSERT запрос
                    StringBuilder insertQuery = new StringBuilder(
                            "INSERT INTO clients (id, client_type_id, company, source_id, is_active");
                    
                    // Добавляем поля created_at и updated_at, если они указаны
                    boolean hasCreatedAt = client.getCreatedAt() != null;
                    boolean hasUpdatedAt = client.getUpdatedAt() != null;
                    
                    if (hasCreatedAt) {
                        insertQuery.append(", created_at");
                    }
                    if (hasUpdatedAt) {
                        insertQuery.append(", updated_at");
                    }
                    
                    insertQuery.append(") VALUES (:id, :clientTypeId, :company, :sourceId, :isActive");
                    
                    if (hasCreatedAt) {
                        insertQuery.append(", :createdAt");
                    }
                    if (hasUpdatedAt) {
                        insertQuery.append(", :updatedAt");
                    }
                    
                    insertQuery.append(")");
                    
                    jakarta.persistence.Query query = entityManager.createNativeQuery(insertQuery.toString())
                            .setParameter("id", specifiedId)
                            .setParameter("clientTypeId", client.getClientType().getId())
                            .setParameter("company", client.getCompany())
                            .setParameter("sourceId", client.getSource())
                            .setParameter("isActive", client.getIsActive() != null ? client.getIsActive() : true);
                    
                    if (hasCreatedAt) {
                        query.setParameter("createdAt", client.getCreatedAt());
                    }
                    if (hasUpdatedAt) {
                        query.setParameter("updatedAt", client.getUpdatedAt());
                    }
                    
                    query.executeUpdate();
                    
                    // Сохраняем связанные fieldValues
                    if (client.getFieldValues() != null && !client.getFieldValues().isEmpty()) {
                        // Загружаем сохраненного клиента для установки связей
                        Client savedClient = entityManager.find(Client.class, specifiedId);
                        for (ClientFieldValue fieldValue : client.getFieldValues()) {
                            fieldValue.setClient(savedClient);
                            entityManager.persist(fieldValue);
                        }
                    }
                } else {
                    // Если ID не указан, используем стандартный метод создания
                    clientCrudService.createClient(client);
                }
            }
            
            // Обновляем AUTO_INCREMENT один раз после всех вставок, если были клиенты с указанными ID
            if (maxSpecifiedId != null) {
                Long currentMaxId = (Long) entityManager.createNativeQuery("SELECT MAX(id) FROM clients").getSingleResult();
                if (currentMaxId != null && maxSpecifiedId >= currentMaxId) {
                    entityManager.createNativeQuery(
                            "ALTER TABLE clients AUTO_INCREMENT = :nextId"
                    )
                    .setParameter("nextId", maxSpecifiedId + 1)
                    .executeUpdate();
                }
            }
            
            return "Успішно імпортовано " + clientsToCreate.size() + " клієнтів";
            
        } catch (IOException e) {
            throw new ClientException("Помилка читання Excel файлу: " + e.getMessage());
        }
    }
    
    private Map<String, Integer> parseHeaders(Row headerRow, List<ClientTypeField> fields, ClientType clientType) {
        Map<String, Integer> columnIndexMap = new HashMap<>();
        
        // Получаем название поля компании из типа клиента
        String companyFieldLabel = clientType.getNameFieldLabel() != null ? 
                clientType.getNameFieldLabel() : "Компанія";
        
        // Сначала создаем мапу всех fieldLabel для быстрого поиска
        Map<String, ClientTypeField> fieldLabelMap = new HashMap<>();
        for (ClientTypeField field : fields) {
            fieldLabelMap.put(field.getFieldLabel(), field);
        }
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellValueAsString(cell);
                if (headerValue == null) {
                    continue;
                }
                headerValue = headerValue.trim();
                
                // Сначала проверяем, не является ли это динамическим полем
                // Используем нормализацию (приведение к нижнему регистру и нормализация пробелов) для более гибкого сравнения
                boolean isDynamicField = false;
                String normalizedHeader = headerValue.replaceAll("\\s+", " ").trim().toLowerCase();
                
                for (ClientTypeField field : fields) {
                    String fieldLabel = field.getFieldLabel();
                    String normalizedFieldLabel = fieldLabel.replaceAll("\\s+", " ").trim().toLowerCase();
                    
                    // Проверяем точное совпадение или начало с названия поля (для случаев типа "Назва підприємства (через кому)")
                    if (normalizedHeader.equals(normalizedFieldLabel) || 
                        normalizedHeader.startsWith(normalizedFieldLabel + " ") ||
                        normalizedHeader.startsWith(normalizedFieldLabel + "(") ||
                        normalizedHeader.startsWith(normalizedFieldLabel + " (")) {
                        columnIndexMap.put("field_" + field.getId(), i);
                        isDynamicField = true;
                        break;
                    }
                }
                
                // Если это не динамическое поле, проверяем статические поля
                if (!isDynamicField) {
                    if (headerValue.contains("ID") && !headerValue.contains("field_")) {
                        columnIndexMap.put("id", i);
                    } else if (headerValue.equals(companyFieldLabel) || 
                               (headerValue.startsWith(companyFieldLabel + " ") && 
                                !fieldLabelMap.containsKey(headerValue))) {
                        // Используем точное название поля компании из типа клиента
                        columnIndexMap.put("company", i);
                    } else if (headerValue.contains("Залучення")) {
                        columnIndexMap.put("source", i);
                    } else if (headerValue.contains("створення") || headerValue.contains("Created")) {
                        columnIndexMap.put("createdAt", i);
                    } else if (headerValue.contains("оновлення") || headerValue.contains("Updated")) {
                        columnIndexMap.put("updatedAt", i);
                    } else if (headerValue.contains("Активний") || headerValue.contains("Active")) {
                        columnIndexMap.put("isActive", i);
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
            if (idCell != null) {
                Long id = parseIdFromCell(idCell);
                if (id != null) {
                    if (clientRepository.existsById(id)) {
                        throw new ClientException("ID " + id + " вже зайнятий");
                    }
                    client.setId(id);
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
        
        // Source - ищем только по названию (как для списков)
        Integer sourceCol = columnIndexMap.get("source");
        if (sourceCol != null) {
            Cell sourceCell = row.getCell(sourceCol);
            String sourceValue = getCellValueAsString(sourceCell);
            if (sourceValue != null && !sourceValue.trim().isEmpty()) {
                String trimmedSource = sourceValue.trim();
                
                // Ищем источник по названию (без учета регистра)
                List<org.example.clientservice.models.field.Source> sources = sourceService.getAllSources();
                org.example.clientservice.models.field.Source foundSource = sources.stream()
                        .filter(s -> s.getName() != null && s.getName().trim().equalsIgnoreCase(trimmedSource))
                        .findFirst()
                        .orElse(null);
                
                if (foundSource == null) {
                    throw new ClientException("Залучення з назвою '" + trimmedSource + "' не знайдено");
                }
                client.setSource(foundSource.getId());
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
                
                // Проверяем, что значение не пустое (учитываем пробелы)
                if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                    List<ClientFieldValue> fieldValues = parseFieldValue(client, field, fieldValue.trim(), rowNumber);
                    client.getFieldValues().addAll(fieldValues);
                } else {
                    if (field.getIsRequired() != null && field.getIsRequired()) {
                        throw new ClientException("Поле '" + field.getFieldLabel() + "' є обов'язковим");
                    }
                }
            } else {
                log.warn("Field '{}' (id={}) column not found in header map. Available columns: {}", 
                        field.getFieldLabel(), field.getId(), 
                        columnIndexMap.entrySet().stream()
                                .filter(e -> e.getKey().startsWith("field_"))
                                .map(e -> e.getKey() + ":" + e.getValue())
                                .collect(Collectors.joining(", ")));
                if (field.getIsRequired() != null && field.getIsRequired()) {
                    throw new ClientException("Поле '" + field.getFieldLabel() + "' є обов'язковим (колонка не знайдена)");
                }
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
    
    private Long parseIdFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    // Для числовых ячеек проверяем, является ли число целым
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return (long) numericValue;
                    } else {
                        // Если число не целое, возвращаем null (ID должен быть целым числом)
                        return null;
                    }
                case STRING:
                    String stringValue = cell.getStringCellValue().trim();
                    if (stringValue.isEmpty()) {
                        return null;
                    }
                    // Пробуем распарсить как Long
                    try {
                        return Long.parseLong(stringValue);
                    } catch (NumberFormatException e) {
                        // Если не получилось, пробуем как double и проверяем, целое ли это
                        try {
                            double doubleValue = Double.parseDouble(stringValue);
                            if (doubleValue == (long) doubleValue) {
                                return (long) doubleValue;
                            } else {
                                // Если число не целое, возвращаем null
                                return null;
                            }
                        } catch (NumberFormatException ex) {
                            // Если не число вообще, возвращаем null (возможно, это значение из другой колонки)
                            return null;
                        }
                    }
                case FORMULA:
                    // Для формул пытаемся получить вычисленное значение
                    try {
                        switch (cell.getCachedFormulaResultType()) {
                            case NUMERIC:
                                double formulaNumericValue = cell.getNumericCellValue();
                                if (formulaNumericValue == (long) formulaNumericValue) {
                                    return (long) formulaNumericValue;
                                } else {
                                    return null;
                                }
                            case STRING:
                                String formulaStringValue = cell.getStringCellValue().trim();
                                if (formulaStringValue.isEmpty()) {
                                    return null;
                                }
                                try {
                                    return Long.parseLong(formulaStringValue);
                                } catch (NumberFormatException e) {
                                    try {
                                        double doubleValue = Double.parseDouble(formulaStringValue);
                                        if (doubleValue == (long) doubleValue) {
                                            return (long) doubleValue;
                                        } else {
                                            return null;
                                        }
                                    } catch (NumberFormatException ex) {
                                        // Если не число, возвращаем null
                                        return null;
                                    }
                                }
                            default:
                                return null;
                        }
                    } catch (Exception e) {
                        // В случае любой ошибки возвращаем null
                        return null;
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            // В случае любой ошибки возвращаем null вместо выбрасывания исключения
            return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        try {
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
                    // Для формул пытаемся получить вычисленное значение
                    try {
                        switch (cell.getCachedFormulaResultType()) {
                            case STRING:
                                return cell.getStringCellValue();
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    return cell.getLocalDateTimeCellValue().format(DATE_TIME_FORMATTER);
                                } else {
                                    double numericValue = cell.getNumericCellValue();
                                    if (numericValue == (long) numericValue) {
                                        return String.valueOf((long) numericValue);
                                    } else {
                                        return String.valueOf(numericValue);
                                    }
                                }
                            case BOOLEAN:
                                return String.valueOf(cell.getBooleanCellValue());
                            default:
                                return cell.getCellFormula();
                        }
                    } catch (Exception e) {
                        return cell.getCellFormula();
                    }
                case BLANK:
                    return null;
                default:
                    // Пробуем получить значение как строку
                    try {
                        DataFormatter formatter = new DataFormatter();
                        return formatter.formatCellValue(cell);
                    } catch (Exception e) {
                        return null;
                    }
            }
        } catch (Exception e) {
            // В случае ошибки пробуем использовать DataFormatter как последний вариант
            try {
                DataFormatter formatter = new DataFormatter();
                return formatter.formatCellValue(cell);
            } catch (Exception ex) {
                return null;
            }
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

