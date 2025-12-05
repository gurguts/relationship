package org.example.clientservice.services.clienttype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldConfig;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StaticFieldsHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static StaticFieldsConfig parseStaticFieldsConfig(ClientType clientType) {
        if (clientType == null || clientType.getStaticFieldsConfig() == null || clientType.getStaticFieldsConfig().trim().isEmpty()) {
            return null; // Если конфигурация не сохранена, возвращаем null - статические поля не будут добавлены
        }

        try {
            return objectMapper.readValue(clientType.getStaticFieldsConfig(), StaticFieldsConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse static fields config for client type {}: {}", clientType.getId(), e.getMessage());
            return null; // При ошибке парсинга возвращаем null
        }
    }

    public static List<ClientTypeFieldDTO> createStaticFieldDTOs(StaticFieldsConfig config) {
        List<ClientTypeFieldDTO> staticFields = new ArrayList<>();

        if (config.getCompany() != null && Boolean.TRUE.equals(config.getCompany().getIsVisible())) {
            staticFields.add(createStaticFieldDTO("company", config.getCompany()));
        }

        if (config.getSource() != null && Boolean.TRUE.equals(config.getSource().getIsVisible())) {
            staticFields.add(createStaticFieldDTO("source", config.getSource()));
        }

        if (config.getCreatedAt() != null && Boolean.TRUE.equals(config.getCreatedAt().getIsVisible())) {
            staticFields.add(createStaticFieldDTO("createdAt", config.getCreatedAt()));
        }

        if (config.getUpdatedAt() != null && Boolean.TRUE.equals(config.getUpdatedAt().getIsVisible())) {
            staticFields.add(createStaticFieldDTO("updatedAt", config.getUpdatedAt()));
        }

        return staticFields;
    }

    private static ClientTypeFieldDTO createStaticFieldDTO(String staticFieldName, StaticFieldConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("StaticFieldConfig cannot be null for field: " + staticFieldName);
        }
        
        ClientTypeFieldDTO dto = new ClientTypeFieldDTO();
        // Используем отрицательные ID для статических полей, чтобы не конфликтовать с реальными полями
        dto.setId((long) -getStaticFieldId(staticFieldName));
        dto.setFieldName(staticFieldName);
        dto.setFieldLabel(config.getFieldLabel() != null && !config.getFieldLabel().trim().isEmpty() 
                ? config.getFieldLabel() 
                : getDefaultLabel(staticFieldName));
        dto.setFieldType(getFieldType(staticFieldName));
        dto.setIsVisibleInTable(true);
        dto.setIsVisibleInCreate(false);
        dto.setIsSearchable(false);
        dto.setIsFilterable(false);
        dto.setDisplayOrder(config.getDisplayOrder() != null ? config.getDisplayOrder() : 999);
        dto.setColumnWidth(config.getColumnWidth());
        dto.setIsStatic(true);
        dto.setStaticFieldName(staticFieldName);
        return dto;
    }

    private static int getStaticFieldId(String staticFieldName) {
        return switch (staticFieldName) {
            case "company" -> 1;
            case "source" -> 2;
            case "createdAt" -> 3;
            case "updatedAt" -> 4;
            default -> 0;
        };
    }

    private static String getDefaultLabel(String staticFieldName) {
        return switch (staticFieldName) {
            case "company" -> "Компанія";
            case "source" -> "Залучення";
            case "createdAt" -> "Створено";
            case "updatedAt" -> "Оновлено";
            default -> staticFieldName;
        };
    }

    private static String getFieldType(String staticFieldName) {
        return switch (staticFieldName) {
            case "company", "source" -> "TEXT";
            case "createdAt", "updatedAt" -> "DATE";
            default -> "TEXT";
        };
    }
}

