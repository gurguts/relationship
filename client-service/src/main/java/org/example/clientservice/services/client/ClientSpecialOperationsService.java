package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.repositories.ClientRepository;
import org.example.clientservice.services.impl.*;
import org.example.clientservice.services.clienttype.ClientTypeFieldService;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSpecialOperationsService implements IClientSpecialOperationsService {
    @PersistenceContext
    private EntityManager entityManager;

    private static final Set<String> VALID_STATIC_FIELDS = Set.of(
            "id", "company", "createdAt", "updatedAt", "source");


    private final ClientRepository clientRepository;
    private final ISourceService sourceService;
    private final ClientTypeFieldService clientTypeFieldService;

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
        // Для экспорта загружаем все источники для маппинга ID на названия в Excel
        // sourceIds используется только для фильтрации при поиске, но sourceDTOs нужны всегда для заполнения поля "Залучення"
        FilterIds filterIds;
        if (query != null && !query.trim().isEmpty()) {
            filterIds = fetchFilterIds(query);
        } else {
            // Загружаем все источники для маппинга, но sourceIds оставляем пустым для фильтрации
            List<Source> allSources = sourceService.getAllSources();
            filterIds = new FilterIds(allSources, List.of());
        }

        List<Client> clientList = fetchClients(query, filterParams, filterIds, sort);

        Workbook workbook = generateWorkbook(clientList, selectedFields, filterIds);

        return convertWorkbookToBytes(workbook);
    }

    private void validateInputs(String query, Map<String, List<String>> filterParams, List<String> selectedFields) {
        if (query != null && query.length() > 255) {
            throw new ClientException("INVALID_QUERY", "Search query cannot exceed 255 characters");
        }
        if (filterParams != null) {
            // Валидация ключей фильтров:
            // - Стандартные ключи (createdAtFrom, createdAtTo, updatedAtFrom, updatedAtTo, source, showInactive, clientTypeId)
            // - Диапазоны (ключи, заканчивающиеся на From/To)
            // - Динамические поля (обрабатываются в ClientSpecification)
            // Все остальные ключи будут проигнорированы в ClientSpecification
        }
        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }
        // Проверяем, что все поля либо статические, либо динамические (формат field_<id>)
        for (String field : selectedFields) {
            if (!VALID_STATIC_FIELDS.contains(field) && !field.startsWith("field_")) {
                throw new ClientException("INVALID_FIELDS", String.format("Invalid field specified for export: %s", field));
            }
            // Если это динамическое поле, проверяем что fieldId существует
            if (field.startsWith("field_")) {
                try {
                    Long fieldId = Long.parseLong(field.substring(6));
                    clientTypeFieldService.getFieldById(fieldId); // Проверяем существование поля
                } catch (Exception e) {
                    throw new ClientException("INVALID_FIELDS", String.format("Dynamic field not found: %s", field));
                }
            }
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


    private List<Client> fetchClients(String query, Map<String, List<String>> filterParams, FilterIds filterIds,
                                      Sort sort) {
        // Нормализуем query: если это строка "null" или пустая строка, преобразуем в null
        String normalizedQuery = null;
        if (query != null) {
            String trimmed = query.trim();
            if (!trimmed.isEmpty() && !"null".equalsIgnoreCase(trimmed)) {
                normalizedQuery = query;
            }
        }
        
        Long clientTypeId = null;
        if (filterParams != null && filterParams.containsKey("clientTypeId")) {
            List<String> clientTypeIdList = filterParams.get("clientTypeId");
            if (clientTypeIdList != null && !clientTypeIdList.isEmpty()) {
                try {
                    clientTypeId = Long.parseLong(clientTypeIdList.get(0));
                } catch (NumberFormatException e) {
                    // Игнорируем невалидный clientTypeId
                }
            }
        }

        // Для экспорта передаем null для sourceIds, если нет поискового запроса
        // Это позволяет экспортировать всех клиентов, а не только тех, которые соответствуют sourceIds
        List<Long> sourceIdsForSpec = (normalizedQuery != null && !normalizedQuery.trim().isEmpty()) 
            ? filterIds.sourceIds() 
            : null;
        
        Specification<Client> spec = new ClientSpecification(
                normalizedQuery,
                filterParams,
                sourceIdsForSpec,
                clientTypeId
        );
        
        // Используем двухэтапный подход:
        // 1. Сначала получаем клиентов через репозиторий с спецификацией (как в ClientSearchService)
        // Это гарантирует, что все фильтры и поиск работают правильно
        // 2. Затем загружаем клиентов по ID с FETCH для связанных объектов
        
        // Этап 1: Получаем клиентов через репозиторий с спецификацией
        // Используем Pageable для получения всех результатов с правильной сортировкой
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort != null ? sort : Sort.unsorted());
        Page<Client> clientPage = clientRepository.findAll(spec, pageable);
        List<Client> clientsWithoutFieldValues = clientPage.getContent();
        
        if (clientsWithoutFieldValues.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Получаем ID клиентов для загрузки с FETCH
        List<Long> clientIds = clientsWithoutFieldValues.stream()
                .map(Client::getId)
                .collect(Collectors.toList());
        
        // Этап 2: Загружаем клиентов по ID с FETCH для связанных объектов
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Client> cq = cb.createQuery(Client.class);
        Root<Client> root = cq.from(Client.class);
        
        // Добавляем FETCH для загрузки связанных объектов
        root.fetch("clientType", JoinType.LEFT);
        
        // JOIN FETCH для загрузки fieldValues и связанных объектов
        // ВАЖНО: Hibernate не может одновременно загружать несколько коллекций типа List через JOIN FETCH
        // Поэтому НЕ загружаем listValues через JOIN FETCH, загрузим его отдельно после запроса
        Fetch<Object, Object> fieldValuesFetch = root.fetch("fieldValues", JoinType.LEFT);
        fieldValuesFetch.fetch("field", JoinType.LEFT); // Загружаем field, но НЕ listValues
        fieldValuesFetch.fetch("valueList", JoinType.LEFT); // Загружаем valueList
        
        // Используем DISTINCT для избежания дубликатов из-за JOIN FETCH
        cq.distinct(true);
        
        // Фильтруем по ID клиентов
        cq.where(root.get("id").in(clientIds));
        
        // Применяем сортировку
        if (sort != null) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order order : sort) {
                Path<?> path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            cq.orderBy(orders);
        }
        
        TypedQuery<Client> typedQuery = entityManager.createQuery(cq);
        List<Client> clients = typedQuery.getResultList();
        
        // Инициализируем listValues отдельно для каждого field, чтобы избежать MultipleBagFetchException
        // Это необходимо, так как Hibernate не может одновременно загружать несколько коллекций типа List через JOIN FETCH
        clients.forEach(client -> {
            if (client.getFieldValues() != null) {
                client.getFieldValues().forEach(fv -> {
                    if (fv.getField() != null) {
                        // Инициализируем listValues отдельно (ленивая загрузка)
                        if (fv.getField().getListValues() != null) {
                            fv.getField().getListValues().size(); // Инициализируем коллекцию
                        }
                    }
                    // valueList уже загружен через JOIN FETCH
                });
            }
        });
        
        return clients;
    }
    

    private Workbook generateWorkbook(List<Client> clientList, List<String> selectedFields, FilterIds filterIds) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Client Data");

        Map<String, String> fieldToHeader = createFieldToHeaderMap(selectedFields);
        createHeaderRow(sheet, selectedFields, fieldToHeader);
        fillDataRows(sheet, clientList, selectedFields, filterIds);

        return workbook;
    }

    private Map<String, String> createFieldToHeaderMap(List<String> selectedFields) {
        Map<String, String> headerMap = new HashMap<>();
        
        // Базовые поля
        headerMap.put("id", "Id");
        headerMap.put("company", "Компанія");
        headerMap.put("createdAt", "Дата створення");
        headerMap.put("updatedAt", "Дата оновлення");
        headerMap.put("source", "Залучення");
        
        // Динамические поля
        for (String field : selectedFields) {
            if (field.startsWith("field_")) {
                try {
                    Long fieldId = Long.parseLong(field.substring(6));
                    ClientTypeField clientTypeField = clientTypeFieldService.getFieldById(fieldId);
                    headerMap.put(field, clientTypeField.getFieldLabel());
                } catch (Exception e) {
                    log.warn("Failed to get field label for field {}: {}", field, e.getMessage());
                    headerMap.put(field, field);
                }
            }
        }
        
        return headerMap;
    }

    private void createHeaderRow(Sheet sheet, List<String> selectedFields, Map<String, String> fieldToHeader) {
        Row headerRow = sheet.createRow(0);
        int colIndex = 0;
        for (String field : selectedFields) {
            String header = fieldToHeader.getOrDefault(field, field);
            headerRow.createCell(colIndex++).setCellValue(header);
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
        if (field.startsWith("field_")) {
            // Динамическое поле
            try {
                Long fieldId = Long.parseLong(field.substring(6));
                return getDynamicFieldValue(client, fieldId);
            } catch (Exception e) {
                log.warn("Failed to get dynamic field value for field {}: {}", field, e.getMessage());
                return "";
            }
        }
        
        // Статические поля
        return switch (field) {
            case "id" -> client.getId() != null ? String.valueOf(client.getId()) : "";
            case "company" -> client.getCompany() != null ? client.getCompany() : "";
            case "createdAt" -> client.getCreatedAt() != null ? client.getCreatedAt().toString() : "";
            case "updatedAt" -> client.getUpdatedAt() != null ? client.getUpdatedAt().toString() : "";
            case "source" -> {
                if (client.getSource() == null) {
                    yield "";
                }
                yield filterIds.sourceDTOs().stream()
                        .filter(source -> source.getId() != null && source.getId().equals(client.getSource()))
                        .findFirst()
                        .map(Source::getName)
                        .orElse("");
            }
            default -> "";
        };
    }
    
    private String getDynamicFieldValue(Client client, Long fieldId) {
        if (client.getFieldValues() == null || client.getFieldValues().isEmpty()) {
            return "";
        }
        
        List<ClientFieldValue> fieldValues = client.getFieldValues().stream()
                .filter(fv -> fv.getField() != null && fv.getField().getId().equals(fieldId))
                .sorted(Comparator.comparingInt(fv -> fv.getDisplayOrder() != null ? fv.getDisplayOrder() : 0))
                .collect(Collectors.toList());
        
        if (fieldValues.isEmpty()) {
            return "";
        }
        
        ClientTypeField field = fieldValues.get(0).getField();
        if (field == null) {
            return "";
        }
        
        // Если поле поддерживает множественные значения, объединяем их
        if (field.getAllowMultiple() != null && field.getAllowMultiple() && fieldValues.size() > 1) {
            return fieldValues.stream()
                    .map(fv -> formatFieldValue(fv, field))
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
        } else {
            return formatFieldValue(fieldValues.get(0), field);
        }
    }
    
    private String formatFieldValue(ClientFieldValue fieldValue, ClientTypeField field) {
        if (field == null) {
            return "";
        }
        
        return switch (field.getFieldType()) {
            case TEXT, PHONE -> fieldValue.getValueText() != null ? fieldValue.getValueText() : "";
            case NUMBER -> fieldValue.getValueNumber() != null ? String.valueOf(fieldValue.getValueNumber()) : "";
            case DATE -> fieldValue.getValueDate() != null ? fieldValue.getValueDate().toString() : "";
            case BOOLEAN -> {
                if (fieldValue.getValueBoolean() == null) yield "";
                yield fieldValue.getValueBoolean() ? "Так" : "Ні";
            }
            case LIST -> {
                if (fieldValue.getValueList() != null && fieldValue.getValueList().getValue() != null) {
                    yield fieldValue.getValueList().getValue();
                }
                yield "";
            }
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

