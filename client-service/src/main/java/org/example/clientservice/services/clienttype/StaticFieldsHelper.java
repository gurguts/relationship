package org.example.clientservice.services.clienttype;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.models.dto.clienttype.ClientTypeFieldDTO;
import org.example.clientservice.models.dto.clienttype.StaticFieldConfig;
import org.example.clientservice.models.dto.clienttype.StaticFieldsConfig;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class StaticFieldsHelper {
    private static final int DEFAULT_DISPLAY_ORDER = 999;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectReader configReader = objectMapper.readerFor(StaticFieldsConfig.class);

    public static StaticFieldsConfig parseStaticFieldsConfig(ClientType clientType) {
        if (clientType == null) {
            log.warn("ClientType is null, cannot parse static fields config");
            return null;
        }
        
        String configJson = clientType.getStaticFieldsConfig();
        if (configJson == null || configJson.trim().isEmpty()) {
            return null;
        }

        try {
            return configReader.readValue(configJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse static fields config for client type {}: {}", 
                    clientType.getId(), e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error parsing static fields config for client type {}: {}", 
                    clientType.getId(), e.getMessage(), e);
            return null;
        }
    }

    public static List<ClientTypeFieldDTO> createStaticFieldDTOs(@NonNull StaticFieldsConfig config) {

        return Stream.of(
                createFieldIfVisible(StaticField.COMPANY, config.getCompany()),
                createFieldIfVisible(StaticField.SOURCE, config.getSource()),
                createFieldIfVisible(StaticField.CREATED_AT, config.getCreatedAt()),
                createFieldIfVisible(StaticField.UPDATED_AT, config.getUpdatedAt())
        )
        .filter(Objects::nonNull)
        .toList();
    }

    private static ClientTypeFieldDTO createFieldIfVisible(StaticField staticField, StaticFieldConfig config) {
        if (config == null || !Boolean.TRUE.equals(config.getIsVisible())) {
            return null;
        }
        return createStaticFieldDTO(staticField, config);
    }

    private static ClientTypeFieldDTO createStaticFieldDTO(@NonNull StaticField staticField, 
                                                          @NonNull StaticFieldConfig config) {
        ClientTypeFieldDTO dto = new ClientTypeFieldDTO();
        dto.setId((long) -staticField.getFieldId());
        dto.setFieldName(staticField.getFieldName());
        dto.setFieldLabel(getFieldLabel(config, staticField));
        dto.setFieldType(staticField.getFieldType());
        dto.setIsVisibleInTable(true);
        dto.setIsVisibleInCreate(false);
        dto.setIsSearchable(false);
        dto.setIsFilterable(false);
        dto.setDisplayOrder(config.getDisplayOrder() != null ? config.getDisplayOrder() : DEFAULT_DISPLAY_ORDER);
        dto.setColumnWidth(config.getColumnWidth());
        dto.setIsStatic(true);
        dto.setStaticFieldName(staticField.getFieldName());
        return dto;
    }
    
    private static String getFieldLabel(@NonNull StaticFieldConfig config, @NonNull StaticField staticField) {
        String label = config.getFieldLabel();
        if (label != null && !label.trim().isEmpty()) {
            return label;
        }
        return staticField.getDefaultLabel();
    }
}
