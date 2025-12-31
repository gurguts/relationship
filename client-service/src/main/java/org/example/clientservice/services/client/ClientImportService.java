package org.example.clientservice.services.client;

import lombok.NonNull;
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
import org.example.clientservice.models.field.Source;
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

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_TIME_NO_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String HEADER_ID = "ID (опціонально)";
    private static final String HEADER_SOURCE = "Залучення (назва)";
    private static final String HEADER_CREATED_AT = "Дата створення (yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss або yyyy-MM-dd'T'HH:mm)";
    private static final String HEADER_UPDATED_AT = "Дата оновлення (yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss або yyyy-MM-dd'T'HH:mm)";
    private static final String HEADER_IS_ACTIVE = "Активний (Так/Ні)";
    private static final String HEADER_MULTIPLE_SUFFIX = " (через кому, якщо кілька)";
    private static final String DEFAULT_COMPANY_LABEL = "Компанія";
    private static final String EXAMPLE_COMPANY = "Приклад компанії";
    private static final String EXAMPLE_TEXT = "Приклад тексту";
    private static final String EXAMPLE_NUMBER = "123.45";
    private static final String EXAMPLE_DATE = "2024-01-01";
    private static final String EXAMPLE_PHONE = "+380123456789";
    private static final String EXAMPLE_BOOLEAN = "Так";
    private static final String SHEET_NAME = "Clients";
    private static final int MIN_ROWS_REQUIRED = 2;
    private static final int EXAMPLE_ROW_INDEX = 1;
    private static final int HEADER_ROW_INDEX = 0;
    private static final boolean DEFAULT_IS_ACTIVE = true;
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_COMPANY = "company";
    private static final String COLUMN_SOURCE = "source";
    private static final String COLUMN_CREATED_AT = "createdAt";
    private static final String COLUMN_UPDATED_AT = "updatedAt";
    private static final String COLUMN_IS_ACTIVE = "isActive";
    private static final String FIELD_PREFIX = "field_";
    private static final String KEYWORD_SOURCE_UA = "Залучення";
    private static final String KEYWORD_CREATED_UA = "створення";
    private static final String KEYWORD_UPDATED_UA = "оновлення";
    private static final String KEYWORD_ACTIVE_UA = "Активний";
    private static final String KEYWORD_CREATED_EN = "Created";
    private static final String KEYWORD_UPDATED_EN = "Updated";
    private static final String KEYWORD_ACTIVE_EN = "Active";
    private static final String BOOLEAN_TRUE_UA = "Так";
    private static final String BOOLEAN_FALSE_UA = "Ні";
    private static final String BOOLEAN_TRUE_EN = "true";
    private static final String BOOLEAN_FALSE_EN = "false";
    private static final String BOOLEAN_TRUE_NUM = "1";
    private static final String BOOLEAN_FALSE_NUM = "0";
    private static final String SQL_INSERT_CLIENTS = "INSERT INTO clients (id, client_type_id, company, source_id, is_active";
    private static final String SQL_VALUES = ") VALUES (:id, :clientTypeId, :company, :sourceId, :isActive";
    private static final String SQL_SELECT_MAX_ID = "SELECT MAX(id) FROM clients";
    private static final String SQL_ALTER_AUTO_INCREMENT = "ALTER TABLE clients AUTO_INCREMENT = :nextId";
    private static final String CONTENT_TYPE_EXCEL_OLD = "application/vnd.ms-excel";
    private static final String CONTENT_TYPE_EXCEL_NEW = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String FILE_EXTENSION_XLSX = ".xlsx";
    private static final String FILE_EXTENSION_XLS = ".xls";
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final String COMMA_SEPARATOR = ",";
    private static final String EMPTY_STRING = "";

    private static final Set<String> BOOLEAN_TRUE_VALUES = Set.of(
            BOOLEAN_TRUE_UA.toLowerCase(), BOOLEAN_TRUE_EN.toLowerCase(), BOOLEAN_TRUE_NUM);
    private static final Set<String> BOOLEAN_FALSE_VALUES = Set.of(
            BOOLEAN_FALSE_UA.toLowerCase(), BOOLEAN_FALSE_EN.toLowerCase(), BOOLEAN_FALSE_NUM);

    @PersistenceContext
    private EntityManager entityManager;

    private final ClientTypeService clientTypeService;
    private final ClientTypeFieldService clientTypeFieldService;
    private final ClientRepository clientRepository;
    private final IClientCrudService clientCrudService;
    private final ISourceService sourceService;

    @Override
    public byte[] generateTemplate(@NonNull Long clientTypeId) {
        log.info("Generating import template for client type: {}", clientTypeId);

        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
        List<ClientTypeField> fields = clientTypeFieldService.getFieldsByClientTypeId(clientTypeId);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(SHEET_NAME);

        createHeaderRow(sheet, clientType, fields);
        createExampleRow(sheet, fields);

        autoSizeColumns(sheet);

        return convertWorkbookToBytes(workbook);
    }

    private void createHeaderRow(@NonNull Sheet sheet, @NonNull ClientType clientType, @NonNull List<ClientTypeField> fields) {
        Row headerRow = sheet.createRow(HEADER_ROW_INDEX);
        int colIndex = 0;

        headerRow.createCell(colIndex++).setCellValue(HEADER_ID);
        String companyLabel = clientType.getNameFieldLabel() != null ? clientType.getNameFieldLabel() : DEFAULT_COMPANY_LABEL;
        headerRow.createCell(colIndex++).setCellValue(companyLabel);
        headerRow.createCell(colIndex++).setCellValue(HEADER_SOURCE);
        headerRow.createCell(colIndex++).setCellValue(HEADER_CREATED_AT);
        headerRow.createCell(colIndex++).setCellValue(HEADER_UPDATED_AT);
        headerRow.createCell(colIndex++).setCellValue(HEADER_IS_ACTIVE);

        for (ClientTypeField field : fields) {
            String header = field.getFieldLabel();
            if (Boolean.TRUE.equals(field.getAllowMultiple())) {
                header += HEADER_MULTIPLE_SUFFIX;
            }
            headerRow.createCell(colIndex++).setCellValue(header);
        }
    }

    private void createExampleRow(@NonNull Sheet sheet, @NonNull List<ClientTypeField> fields) {
        Row exampleRow = sheet.createRow(EXAMPLE_ROW_INDEX);
        int colIndex = 0;

        exampleRow.createCell(colIndex++).setCellValue(EMPTY_STRING);
        exampleRow.createCell(colIndex++).setCellValue(EXAMPLE_COMPANY);
        exampleRow.createCell(colIndex++).setCellValue(EMPTY_STRING);
        exampleRow.createCell(colIndex++).setCellValue(EMPTY_STRING);
        exampleRow.createCell(colIndex++).setCellValue(EMPTY_STRING);
        exampleRow.createCell(colIndex++).setCellValue(EXAMPLE_BOOLEAN);

        for (ClientTypeField field : fields) {
            Cell cell = exampleRow.createCell(colIndex++);
            setExampleCellValue(cell, field);
        }
    }

    private void setExampleCellValue(@NonNull Cell cell, @NonNull ClientTypeField field) {
        switch (field.getFieldType()) {
            case TEXT -> cell.setCellValue(EXAMPLE_TEXT);
            case NUMBER -> cell.setCellValue(EXAMPLE_NUMBER);
            case DATE -> cell.setCellValue(EXAMPLE_DATE);
            case PHONE -> cell.setCellValue(EXAMPLE_PHONE);
            case BOOLEAN -> cell.setCellValue(EXAMPLE_BOOLEAN);
            case LIST -> setListExampleValue(cell, field);
        }
    }

    private void setListExampleValue(@NonNull Cell cell, @NonNull ClientTypeField field) {
        if (field.getListValues() == null || field.getListValues().isEmpty()) {
            return;
        }

        String exampleValue = field.getListValues().get(0).getValue();
        if (Boolean.TRUE.equals(field.getAllowMultiple()) && field.getListValues().size() > 1) {
            exampleValue += COMMA_SEPARATOR + " " + field.getListValues().get(1).getValue();
        }
        cell.setCellValue(exampleValue);
    }

    private void autoSizeColumns(@NonNull Sheet sheet) {
        Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
        if (headerRow != null) {
            int lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importClients(@NonNull Long clientTypeId, @NonNull MultipartFile file) {
        log.info("Starting import for client type: {}, file size: {} bytes", clientTypeId, file.getSize());

        validateImportFile(file);

        ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
        List<ClientTypeField> fields = clientTypeFieldService.getFieldsByClientTypeId(clientTypeId);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            validateSheet(sheet);

            Row headerRow = sheet.getRow(HEADER_ROW_INDEX);
            if (headerRow == null) {
                throw new ClientException("IMPORT_INVALID_FILE", "Header row is missing");
            }

            Map<String, Integer> columnIndexMap = parseHeaders(headerRow, fields, clientType);
            validateRequiredColumns(columnIndexMap);

            Map<String, Source> sourceNameMap = buildSourceNameMap();

            ImportResult importResult = processRows(sheet, clientType, fields, columnIndexMap, sourceNameMap);

            if (!importResult.errors().isEmpty()) {
                throw buildImportErrorsException(importResult.errors());
            }

            saveClients(importResult.clients());

            log.info("Successfully imported {} clients for client type: {}", importResult.clients().size(), clientTypeId);
            return String.format("Successfully imported %d clients", importResult.clients().size());

        } catch (ClientException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error reading Excel file for client type {}: {}", clientTypeId, e.getMessage(), e);
            throw new ClientException("IMPORT_READ_ERROR",
                    String.format("Error reading Excel file: %s", e.getMessage()));
        }
    }

    private void validateSheet(@NonNull Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() < MIN_ROWS_REQUIRED) {
            throw new ClientException("IMPORT_INVALID_FILE",
                    "Excel file must contain at least a header and one row of data");
        }
    }

    private void validateRequiredColumns(@NonNull Map<String, Integer> columnIndexMap) {
        if (!columnIndexMap.containsKey(COLUMN_COMPANY)) {
            throw new ClientException("IMPORT_MISSING_COLUMN",
                    "Required column 'company' not found in header");
        }
    }

    private Map<String, Source> buildSourceNameMap() {
        List<Source> allSources = sourceService.getAllSources();
        return allSources.stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toMap(
                        s -> s.getName().trim().toLowerCase(),
                        s -> s,
                        (s1, s2) -> s1
                ));
    }

    private ImportResult processRows(@NonNull Sheet sheet, @NonNull ClientType clientType,
                                     @NonNull List<ClientTypeField> fields,
                                     @NonNull Map<String, Integer> columnIndexMap,
                                     @NonNull Map<String, Source> sourceNameMap) {
        List<Client> clientsToCreate = new ArrayList<>();
        List<String> rowErrors = new ArrayList<>();

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isEmptyRow(row)) {
                continue;
            }

            try {
                Client client = parseClientRow(row, clientType, fields, columnIndexMap, rowIndex + 1, sourceNameMap);
                clientsToCreate.add(client);
            } catch (ClientException e) {
                String errorMessage = String.format("Рядок %d: %s", rowIndex + 1, e.getMessage());
                rowErrors.add(errorMessage);
                log.warn("Error parsing row {}: {}", rowIndex + 1, e.getMessage());
            } catch (Exception e) {
                String errorMessage = String.format("Рядок %d: Unexpected error - %s", rowIndex + 1, e.getMessage());
                rowErrors.add(errorMessage);
                log.warn("Unexpected error parsing row {}: {}", rowIndex + 1, e.getMessage(), e);
            }
        }

        return new ImportResult(clientsToCreate, rowErrors);
    }

    private boolean isEmptyRow(@NonNull Row row) {
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum <= 0) {
            return true;
        }

        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null) {
                String cellValue = getCellValueAsString(cell);
                if (cellValue != null && !cellValue.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private ClientException buildImportErrorsException(@NonNull List<String> rowErrors) {
        String errorDetails = String.join("\n", rowErrors);
        return new ClientException("IMPORT_ERRORS",
                String.format("Import errors (%d rows):\n%s", rowErrors.size(), errorDetails));
    }

    private void saveClients(@NonNull List<Client> clients) {
        if (clients.isEmpty()) {
            log.warn("No clients to save");
            return;
        }

        log.info("Saving {} clients", clients.size());

        Long maxSpecifiedId = findMaxSpecifiedId(clients);

        for (Client client : clients) {
            if (client.getId() != null) {
                saveClientWithId(client, client.getId());
            } else {
                clientCrudService.createClient(client);
            }
        }

        if (maxSpecifiedId != null) {
            updateAutoIncrement(maxSpecifiedId);
        }

        log.info("Successfully saved {} clients", clients.size());
    }

    private Long findMaxSpecifiedId(@NonNull List<Client> clients) {
        return clients.stream()
                .map(Client::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(null);
    }

    private void saveClientWithId(@NonNull Client client, @NonNull Long specifiedId) {
        validateClientForSave(client);

        jakarta.persistence.Query query = createInsertQuery(client);
        setQueryParameters(query, client, specifiedId);
        executeClientInsert(query);

        saveFieldValues(client, specifiedId);
    }

    private void validateClientForSave(@NonNull Client client) {
        if (client.getClientType() == null || client.getClientType().getId() == null) {
            throw new ClientException("IMPORT_INVALID_DATA", "Client type is required");
        }
    }

    private jakarta.persistence.Query createInsertQuery(@NonNull Client client) {
        String insertQuery = buildInsertQuery(client);
        return entityManager.createNativeQuery(insertQuery);
    }

    private void setQueryParameters(@NonNull jakarta.persistence.Query query,
                                    @NonNull Client client,
                                    @NonNull Long specifiedId) {
        query.setParameter("id", specifiedId)
                .setParameter("clientTypeId", client.getClientType().getId())
                .setParameter("company", client.getCompany())
                .setParameter("sourceId", client.getSourceId())
                .setParameter("isActive", client.getIsActive() != null ? client.getIsActive() : DEFAULT_IS_ACTIVE);

        if (client.getCreatedAt() != null) {
            query.setParameter("createdAt", client.getCreatedAt());
        }
        if (client.getUpdatedAt() != null) {
            query.setParameter("updatedAt", client.getUpdatedAt());
        }
    }

    private void executeClientInsert(@NonNull jakarta.persistence.Query query) {
        query.executeUpdate();
    }

    private String buildInsertQuery(@NonNull Client client) {
        StringBuilder query = new StringBuilder(SQL_INSERT_CLIENTS);
        boolean hasCreatedAt = client.getCreatedAt() != null;
        boolean hasUpdatedAt = client.getUpdatedAt() != null;

        if (hasCreatedAt) {
            query.append(", created_at");
        }
        if (hasUpdatedAt) {
            query.append(", updated_at");
        }

        query.append(SQL_VALUES);

        if (hasCreatedAt) {
            query.append(", :createdAt");
        }
        if (hasUpdatedAt) {
            query.append(", :updatedAt");
        }

        query.append(")");
        return query.toString();
    }

    private void saveFieldValues(@NonNull Client client, @NonNull Long clientId) {
        if (client.getFieldValues() == null || client.getFieldValues().isEmpty()) {
            return;
        }

        Client savedClient = entityManager.find(Client.class, clientId);
        if (savedClient == null) {
            log.warn("Client with id {} not found after save", clientId);
            return;
        }

        for (ClientFieldValue fieldValue : client.getFieldValues()) {
            fieldValue.setClient(savedClient);
            entityManager.persist(fieldValue);
        }
    }

    private void updateAutoIncrement(@NonNull Long maxSpecifiedId) {
        try {
            Long currentMaxId = (Long) entityManager.createNativeQuery(SQL_SELECT_MAX_ID).getSingleResult();
            if (currentMaxId != null && maxSpecifiedId >= currentMaxId) {
                entityManager.createNativeQuery(SQL_ALTER_AUTO_INCREMENT)
                        .setParameter("nextId", maxSpecifiedId + 1)
                        .executeUpdate();
                log.debug("Updated AUTO_INCREMENT to {}", maxSpecifiedId + 1);
            }
        } catch (Exception e) {
            log.warn("Failed to update AUTO_INCREMENT: {}", e.getMessage());
        }
    }

    private Map<String, Integer> parseHeaders(@NonNull Row headerRow, @NonNull List<ClientTypeField> fields, @NonNull ClientType clientType) {
        Map<String, Integer> columnIndexMap = new HashMap<>();

        String companyFieldLabel = clientType.getNameFieldLabel() != null
                ? clientType.getNameFieldLabel() : DEFAULT_COMPANY_LABEL;

        Map<String, ClientTypeField> fieldLabelMap = buildFieldLabelMap(fields);

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) {
                continue;
            }

            String headerValue = getCellValueAsString(cell);
            if (headerValue == null || headerValue.trim().isEmpty()) {
                continue;
            }

            headerValue = headerValue.trim();
            String normalizedHeader = normalizeHeader(headerValue);

            if (tryMapDynamicField(columnIndexMap, fields, normalizedHeader, headerValue, i)) {
                continue;
            }

            mapStaticField(columnIndexMap, headerValue, normalizedHeader, companyFieldLabel, fieldLabelMap, i);
        }

        return columnIndexMap;
    }

    private Map<String, ClientTypeField> buildFieldLabelMap(@NonNull List<ClientTypeField> fields) {
        Map<String, ClientTypeField> fieldLabelMap = new HashMap<>();
        for (ClientTypeField field : fields) {
            fieldLabelMap.put(field.getFieldLabel(), field);
        }
        return fieldLabelMap;
    }

    private String normalizeHeader(@NonNull String header) {
        return header.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private boolean tryMapDynamicField(@NonNull Map<String, Integer> columnIndexMap,
                                       @NonNull List<ClientTypeField> fields,
                                       @NonNull String normalizedHeader,
                                       @NonNull String headerValue,
                                       int columnIndex) {
        for (ClientTypeField field : fields) {
            String fieldLabel = field.getFieldLabel();
            String normalizedFieldLabel = normalizeHeader(fieldLabel);

            if (normalizedHeader.equals(normalizedFieldLabel) ||
                    normalizedHeader.startsWith(normalizedFieldLabel + " ") ||
                    normalizedHeader.startsWith(normalizedFieldLabel + "(") ||
                    normalizedHeader.startsWith(normalizedFieldLabel + " (")) {
                columnIndexMap.put(FIELD_PREFIX + field.getId(), columnIndex);
                return true;
            }
        }
        return false;
    }

    private void mapStaticField(@NonNull Map<String, Integer> columnIndexMap,
                                @NonNull String headerValue,
                                @NonNull String normalizedHeader,
                                @NonNull String companyFieldLabel,
                                @NonNull Map<String, ClientTypeField> fieldLabelMap,
                                int columnIndex) {
        if (headerValue.contains("ID") && !headerValue.contains(FIELD_PREFIX)) {
            columnIndexMap.put(COLUMN_ID, columnIndex);
        } else if (headerValue.equals(companyFieldLabel) ||
                (headerValue.startsWith(companyFieldLabel + " ") && !fieldLabelMap.containsKey(headerValue))) {
            columnIndexMap.put(COLUMN_COMPANY, columnIndex);
        } else if (normalizedHeader.contains(KEYWORD_SOURCE_UA.toLowerCase())) {
            columnIndexMap.put(COLUMN_SOURCE, columnIndex);
        } else if (normalizedHeader.contains(KEYWORD_CREATED_UA.toLowerCase()) || normalizedHeader.contains(KEYWORD_CREATED_EN.toLowerCase())) {
            columnIndexMap.put(COLUMN_CREATED_AT, columnIndex);
        } else if (normalizedHeader.contains(KEYWORD_UPDATED_UA.toLowerCase()) || normalizedHeader.contains(KEYWORD_UPDATED_EN.toLowerCase())) {
            columnIndexMap.put(COLUMN_UPDATED_AT, columnIndex);
        } else if (normalizedHeader.contains(KEYWORD_ACTIVE_UA.toLowerCase()) || normalizedHeader.contains(KEYWORD_ACTIVE_EN.toLowerCase())) {
            columnIndexMap.put(COLUMN_IS_ACTIVE, columnIndex);
        }
    }

    private Client parseClientRow(@NonNull Row row, @NonNull ClientType clientType,
                                  @NonNull List<ClientTypeField> fields,
                                  @NonNull Map<String, Integer> columnIndexMap,
                                  int rowNumber,
                                  @NonNull Map<String, Source> sourceNameMap) {
        Client client = new Client();
        client.setClientType(clientType);
        client.setFieldValues(new ArrayList<>());

        parseId(client, row, columnIndexMap, rowNumber);
        parseCompany(client, row, columnIndexMap, rowNumber);
        parseSource(client, row, columnIndexMap, sourceNameMap, rowNumber);
        parseDateTimeField(client, row, columnIndexMap, COLUMN_CREATED_AT, rowNumber);
        parseDateTimeField(client, row, columnIndexMap, COLUMN_UPDATED_AT, rowNumber);
        parseIsActive(client, row, columnIndexMap, rowNumber);
        parseFieldValues(client, row, fields, columnIndexMap, rowNumber);

        return client;
    }

    private void parseId(@NonNull Client client, @NonNull Row row, @NonNull Map<String, Integer> columnIndexMap, int rowNumber) {
        Integer idCol = columnIndexMap.get(COLUMN_ID);
        if (idCol == null) {
            return;
        }

        Cell idCell = row.getCell(idCol);
        if (idCell == null) {
            return;
        }

        Long id = parseIdFromCell(idCell);
        if (id == null) {
            return;
        }

        if (clientRepository.existsById(id)) {
            throw new ClientException("IMPORT_ID_EXISTS",
                    String.format("Row %d: ID %d is already taken", rowNumber, id));
        }

        client.setId(id);
    }

    private void parseCompany(@NonNull Client client, @NonNull Row row, @NonNull Map<String, Integer> columnIndexMap, int rowNumber) {
        Integer companyCol = columnIndexMap.get(COLUMN_COMPANY);
        if (companyCol == null) {
            throw new ClientException("IMPORT_MISSING_COLUMN",
                    String.format("Row %d: Company name column not found", rowNumber));
        }

        Cell companyCell = row.getCell(companyCol);
        String company = getCellValueAsString(companyCell);
        if (company == null || company.trim().isEmpty()) {
            throw new ClientException("IMPORT_REQUIRED_FIELD",
                    String.format("Row %d: Company name is a required field", rowNumber));
        }

        client.setCompany(company.trim());
    }

    private void parseSource(@NonNull Client client, @NonNull Row row,
                            @NonNull Map<String, Integer> columnIndexMap,
                            @NonNull Map<String, Source> sourceNameMap,
                            int rowNumber) {
        Integer sourceCol = columnIndexMap.get(COLUMN_SOURCE);
        if (sourceCol == null) {
            return;
        }

        Cell sourceCell = row.getCell(sourceCol);
        String sourceValue = getCellValueAsString(sourceCell);
        if (sourceValue == null || sourceValue.trim().isEmpty()) {
            return;
        }

        String trimmedSource = sourceValue.trim();
        Source foundSource = sourceNameMap.get(trimmedSource.toLowerCase());

        if (foundSource == null) {
            throw new ClientException("IMPORT_SOURCE_NOT_FOUND",
                    String.format("Row %d: Source with name '%s' not found", rowNumber, trimmedSource));
        }

        client.setSourceId(foundSource.getId());
    }

    private void parseDateTimeField(@NonNull Client client, @NonNull Row row,
                                   @NonNull Map<String, Integer> columnIndexMap,
                                   @NonNull String columnKey,
                                   int rowNumber) {
        Integer dateTimeCol = columnIndexMap.get(columnKey);
        if (dateTimeCol == null) {
            return;
        }

        Cell dateTimeCell = row.getCell(dateTimeCol);
        String dateTimeValue = getCellValueAsString(dateTimeCell);
        if (dateTimeValue == null || dateTimeValue.trim().isEmpty()) {
            return;
        }

        try {
            LocalDateTime dateTime = parseDateTime(dateTimeValue.trim());
            if (COLUMN_CREATED_AT.equals(columnKey)) {
                client.setCreatedAt(dateTime);
            } else if (COLUMN_UPDATED_AT.equals(columnKey)) {
                client.setUpdatedAt(dateTime);
            }
        } catch (ClientException e) {
            throw new ClientException("IMPORT_INVALID_DATETIME",
                    String.format("Row %d: %s", rowNumber, e.getMessage()));
        }
    }

    private void parseIsActive(@NonNull Client client, @NonNull Row row, @NonNull Map<String, Integer> columnIndexMap, int rowNumber) {
        Integer isActiveCol = columnIndexMap.get(COLUMN_IS_ACTIVE);
        if (isActiveCol != null) {
            Cell isActiveCell = row.getCell(isActiveCol);
            String isActiveValue = getCellValueAsString(isActiveCell);
            if (isActiveValue != null && !isActiveValue.trim().isEmpty()) {
                client.setIsActive(parseBoolean(isActiveValue.trim(), rowNumber));
            } else {
                client.setIsActive(DEFAULT_IS_ACTIVE);
            }
        } else {
            client.setIsActive(DEFAULT_IS_ACTIVE);
        }
    }

    private void parseFieldValues(@NonNull Client client, @NonNull Row row,
                                  @NonNull List<ClientTypeField> fields,
                                  @NonNull Map<String, Integer> columnIndexMap,
                                  int rowNumber) {
        for (ClientTypeField field : fields) {
            Integer fieldCol = columnIndexMap.get(FIELD_PREFIX + field.getId());
            if (fieldCol == null) {
                handleMissingField(field, columnIndexMap, rowNumber);
                continue;
            }

            Cell fieldCell = row.getCell(fieldCol);
            String fieldValue = getCellValueAsString(fieldCell);

            if (fieldValue == null || fieldValue.trim().isEmpty()) {
                validateRequiredField(field, rowNumber);
                continue;
            }

            List<ClientFieldValue> fieldValues = parseFieldValue(client, field, fieldValue.trim(), rowNumber);
            client.getFieldValues().addAll(fieldValues);
        }
    }

    private void handleMissingField(@NonNull ClientTypeField field, @NonNull Map<String, Integer> columnIndexMap, int rowNumber) {
        log.warn("Row {}: Field '{}' (id={}) column not found in header map. Available columns: {}",
                rowNumber, field.getFieldLabel(), field.getId(),
                columnIndexMap.entrySet().stream()
                        .filter(e -> e.getKey().startsWith(FIELD_PREFIX))
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ")));
        validateRequiredField(field, rowNumber);
    }

    private void validateRequiredField(@NonNull ClientTypeField field, int rowNumber) {
        if (Boolean.TRUE.equals(field.getIsRequired())) {
            throw new ClientException("IMPORT_REQUIRED_FIELD",
                    String.format("Row %d: Field '%s' is required", rowNumber, field.getFieldLabel()));
        }
    }

    private List<ClientFieldValue> parseFieldValue(@NonNull Client client, @NonNull ClientTypeField field,
                                                    @NonNull String value, int rowNumber) {
        List<ClientFieldValue> fieldValues = new ArrayList<>();

        if (Boolean.TRUE.equals(field.getAllowMultiple()) && value.contains(COMMA_SEPARATOR)) {
            String[] values = value.split(COMMA_SEPARATOR);
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

    private ClientFieldValue createFieldValue(@NonNull Client client, @NonNull ClientTypeField field,
                                              @NonNull String value, int displayOrder, int rowNumber) {
        ClientFieldValue fieldValue = new ClientFieldValue();
        fieldValue.setClient(client);
        fieldValue.setField(field);
        fieldValue.setDisplayOrder(displayOrder);

        switch (field.getFieldType()) {
            case TEXT, PHONE -> fieldValue.setValueText(value);
            case NUMBER -> setNumberValue(fieldValue, field, value, rowNumber);
            case DATE -> setDateValue(fieldValue, field, value, rowNumber);
            case BOOLEAN -> fieldValue.setValueBoolean(parseBoolean(value, rowNumber));
            case LIST -> setListValue(fieldValue, field, value, rowNumber);
        }

        return fieldValue;
    }

    private void setNumberValue(@NonNull ClientFieldValue fieldValue, @NonNull ClientTypeField field,
                               @NonNull String value, int rowNumber) {
        try {
            BigDecimal numberValue = new BigDecimal(value);
            fieldValue.setValueNumber(numberValue);
        } catch (NumberFormatException e) {
            throw new ClientException("IMPORT_INVALID_NUMBER",
                    String.format("Row %d: Field '%s': invalid number format: %s", rowNumber, field.getFieldLabel(), value));
        }
    }

    private void setDateValue(@NonNull ClientFieldValue fieldValue, @NonNull ClientTypeField field,
                             @NonNull String value, int rowNumber) {
        try {
            LocalDate dateValue = LocalDate.parse(value, DATE_FORMATTER);
            fieldValue.setValueDate(dateValue);
        } catch (DateTimeParseException e) {
            throw new ClientException("IMPORT_INVALID_DATE",
                    String.format("Row %d: Field '%s': invalid date format (expected yyyy-MM-dd): %s", rowNumber, field.getFieldLabel(), value));
        }
    }

    private void setListValue(@NonNull ClientFieldValue fieldValue, @NonNull ClientTypeField field,
                              @NonNull String value, int rowNumber) {
        ClientTypeFieldListValue listValue = findListValue(field, value);
        if (listValue == null) {
            throw new ClientException("IMPORT_INVALID_LIST_VALUE",
                    String.format("Row %d: Field '%s': value '%s' not found in available values list", rowNumber, field.getFieldLabel(), value));
        }
        fieldValue.setValueList(listValue);
    }

    private ClientTypeFieldListValue findListValue(@NonNull ClientTypeField field, @NonNull String value) {
        if (field.getListValues() == null || field.getListValues().isEmpty()) {
            return null;
        }

        String trimmedValue = value.trim();
        return field.getListValues().stream()
                .filter(lv -> lv.getValue() != null && lv.getValue().equalsIgnoreCase(trimmedValue))
                .findFirst()
                .orElse(null);
    }

    private Boolean parseBoolean(@NonNull String value, int rowNumber) {
        String trimmed = value.trim().toLowerCase();
        if (BOOLEAN_TRUE_VALUES.contains(trimmed)) {
            return true;
        } else if (BOOLEAN_FALSE_VALUES.contains(trimmed)) {
            return false;
        } else {
            throw new ClientException("IMPORT_INVALID_BOOLEAN",
                    String.format("Row %d: Invalid boolean value. Expected 'Так' or 'Ні', got: %s", rowNumber, value));
        }
    }

    private LocalDateTime parseDateTime(@NonNull String value) {
        List<DateTimeFormatter> formatters = List.of(
                DATE_TIME_FORMATTER,
                ISO_DATE_TIME_FORMATTER,
                ISO_DATE_TIME_NO_SECONDS_FORMATTER
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
            return date.atTime(LocalTime.MIN);
        } catch (DateTimeParseException e) {
            throw new ClientException("IMPORT_INVALID_DATETIME",
                    "Invalid date/time format. Expected yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd'T'HH:mm");
        }
    }

    private Long parseIdFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> parseNumericId(cell);
                case STRING -> parseStringId(cell);
                case FORMULA -> parseFormulaId(cell);
                default -> null;
            };
        } catch (Exception e) {
            log.debug("Error parsing ID from cell: {}", e.getMessage());
            return null;
        }
    }

    private Long parseNumericId(@NonNull Cell cell) {
        double numericValue = cell.getNumericCellValue();
        if (numericValue == (long) numericValue) {
            return (long) numericValue;
        }
        return null;
    }

    private Long parseStringId(@NonNull Cell cell) {
        String stringValue = cell.getStringCellValue().trim();
        if (stringValue.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException e) {
            return parseDoubleAsLong(stringValue);
        }
    }

    private Long parseDoubleAsLong(@NonNull String stringValue) {
        try {
            double doubleValue = Double.parseDouble(stringValue);
            if (doubleValue == (long) doubleValue) {
                return (long) doubleValue;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private Long parseFormulaId(@NonNull Cell cell) {
        try {
            return switch (cell.getCachedFormulaResultType()) {
                case NUMERIC -> parseNumericId(cell);
                case STRING -> parseStringId(cell);
                default -> null;
            };
        } catch (Exception e) {
            log.debug("Error parsing formula ID from cell: {}", e.getMessage());
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> formatNumericCell(cell);
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> formatFormulaCell(cell);
                case BLANK -> null;
                default -> formatCellWithFormatter(cell);
            };
        } catch (Exception e) {
            log.debug("Error getting cell value as string: {}", e.getMessage());
            return formatCellWithFormatter(cell);
        }
    }

    private String formatNumericCell(@NonNull Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().format(DATE_TIME_FORMATTER);
        }

        double numericValue = cell.getNumericCellValue();
        if (numericValue == (long) numericValue) {
            return String.valueOf((long) numericValue);
        }
        return String.valueOf(numericValue);
    }

    private String formatFormulaCell(@NonNull Cell cell) {
        try {
            return switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> formatNumericCell(cell);
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> cell.getCellFormula();
            };
        } catch (Exception e) {
            log.debug("Error formatting formula cell: {}", e.getMessage());
            return cell.getCellFormula();
        }
    }

    private String formatCellWithFormatter(@NonNull Cell cell) {
        try {
            DataFormatter formatter = new DataFormatter();
            return formatter.formatCellValue(cell);
        } catch (Exception e) {
            log.debug("Error formatting cell with formatter: {}", e.getMessage());
            return null;
        }
    }

    private byte[] convertWorkbookToBytes(@NonNull Workbook workbook) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error converting workbook to bytes: {}", e.getMessage(), e);
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

    private void validateImportFile(@NonNull MultipartFile file) {
        if (file.isEmpty()) {
            throw new ClientException("IMPORT_EMPTY_FILE", "File cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ClientException("IMPORT_FILE_TOO_LARGE",
                    String.format("File size exceeds maximum allowed size of %d bytes", MAX_FILE_SIZE_BYTES));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(FILE_EXTENSION_XLSX) && !filename.toLowerCase().endsWith(FILE_EXTENSION_XLS))) {
            throw new ClientException("IMPORT_INVALID_FILE_TYPE", "Only Excel files (.xlsx, .xls) are supported");
        }

        String contentType = file.getContentType();
        if (contentType != null && !isValidContentType(contentType)) {
            throw new ClientException("IMPORT_INVALID_FILE_TYPE", "File is not a valid Excel file");
        }
    }

    private boolean isValidContentType(@NonNull String contentType) {
        return contentType.contains("spreadsheet") ||
                contentType.contains("excel") ||
                contentType.equals(CONTENT_TYPE_EXCEL_OLD) ||
                contentType.equals(CONTENT_TYPE_EXCEL_NEW);
    }

    private record ImportResult(@NonNull List<Client> clients, @NonNull List<String> errors) {
    }
}
