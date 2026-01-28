package org.example.clientservice.services.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.ClientFilterIds;
import org.example.clientservice.models.field.Source;
import org.example.clientservice.services.impl.ISourceService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientSearchQueryProcessor {

    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final TypeReference<Map<String, List<String>>> FILTERS_TYPE_REFERENCE =
            new TypeReference<>() {
            };

    private final ISourceService sourceService;
    private final ObjectMapper objectMapper;

    public String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(filtersJson, FILTERS_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse filters JSON: {}", e.getMessage(), e);
            throw new ClientException("INVALID_JSON", "Invalid JSON format for filters");
        }
    }

    public ClientFilterIds fetchFilterIds(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ClientFilterIds(Collections.emptyList(), Collections.emptyList());
        }

        List<Source> sourceData = sourceService.findByNameContaining(query);
        List<Long> sourceIds = sourceData.stream()
                .map(Source::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new ClientFilterIds(sourceData, sourceIds);
    }

    public Map<String, List<String>> cleanFilterParamsForPurchase(Map<String, List<String>> filterParams) {
        if (filterParams == null || filterParams.isEmpty()) {
            return Collections.emptyMap();
        }

        if (!filterParams.containsKey(FILTER_KEY_CLIENT_TYPE_ID)) {
            return Collections.unmodifiableMap(filterParams);
        }

        Map<String, List<String>> cleaned = new HashMap<>(filterParams);
        cleaned.remove(FILTER_KEY_CLIENT_TYPE_ID);
        return cleaned;
    }
}
