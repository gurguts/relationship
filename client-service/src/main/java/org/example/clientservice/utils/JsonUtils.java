package org.example.clientservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class JsonUtils {
    private static final String ERROR_INVALID_JSON = "INVALID_JSON";
    
    private static final TypeReference<Map<String, List<String>>> FILTERS_TYPE_REFERENCE =
            new TypeReference<>() {
            };

    private JsonUtils() {
    }

    public static Map<String, List<String>> parseFilters(@NonNull ObjectMapper objectMapper, String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(filtersJson, FILTERS_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            log.error("Error parsing filters JSON: {}", e.getMessage(), e);
            throw new ClientException(ERROR_INVALID_JSON, 
                    String.format("Invalid JSON format for filters: %s", e.getMessage()), e);
        } catch (Exception e) {
            log.error("Unexpected error parsing filters JSON: {}", e.getMessage(), e);
            throw new ClientException(ERROR_INVALID_JSON,
                    String.format("Failed to parse filters JSON: %s", e.getMessage()), e);
        }
    }
}
