package org.example.containerservice.services;

import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClientFilterMapper {

    private static final String FILTER_KEY_SOURCE = "source";
    private static final String FILTER_KEY_CLIENT_SOURCE = "clientSource";
    private static final String FILTER_KEY_CLIENT_PRODUCT = "clientProduct";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_FROM = "clientCreatedAtFrom";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_TO = "clientCreatedAtTo";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_FROM = "clientUpdatedAtFrom";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_TO = "clientUpdatedAtTo";
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final String FILTER_PREFIX_FIELD = "field";
    private static final String MAPPED_KEY_SOURCE = "source";
    private static final String MAPPED_KEY_CREATED_AT_FROM = "createdAtFrom";
    private static final String MAPPED_KEY_CREATED_AT_TO = "createdAtTo";
    private static final String MAPPED_KEY_UPDATED_AT_FROM = "updatedAtFrom";
    private static final String MAPPED_KEY_UPDATED_AT_TO = "updatedAtTo";

    public boolean hasClientFilters(@NonNull Map<String, List<String>> clientFilterParams, String query) {
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasClientFilters = !clientFilterParams.isEmpty();
        return hasClientFilters || hasQuery;
    }

    public Map<String, List<String>> mapClientFilterParams(@NonNull Map<String, List<String>> clientFilterParams) {
        return clientFilterParams.entrySet().stream()
                .filter(entry -> isClientFilterKey(entry.getKey()))
                .collect(Collectors.toMap(
                        entry -> mapClientFilterKey(entry.getKey()),
                        Map.Entry::getValue));
    }

    public boolean isClientFilterKey(String key) {
        return FILTER_KEY_SOURCE.equals(key) ||
                FILTER_KEY_CLIENT_SOURCE.equals(key) ||
                FILTER_KEY_CLIENT_PRODUCT.equals(key) ||
                FILTER_KEY_CLIENT_CREATED_AT_FROM.equals(key) ||
                FILTER_KEY_CLIENT_CREATED_AT_TO.equals(key) ||
                FILTER_KEY_CLIENT_UPDATED_AT_FROM.equals(key) ||
                FILTER_KEY_CLIENT_UPDATED_AT_TO.equals(key) ||
                key != null && key.startsWith(FILTER_PREFIX_FIELD);
    }

    public boolean isContainerFilterKey(String key) {
        return key != null &&
                !FILTER_KEY_CLIENT_TYPE_ID.equals(key) &&
                !FILTER_KEY_SOURCE.equals(key) &&
                !FILTER_KEY_CLIENT_SOURCE.equals(key) &&
                !FILTER_KEY_CLIENT_PRODUCT.equals(key) &&
                !FILTER_KEY_CLIENT_CREATED_AT_FROM.equals(key) &&
                !FILTER_KEY_CLIENT_CREATED_AT_TO.equals(key) &&
                !FILTER_KEY_CLIENT_UPDATED_AT_FROM.equals(key) &&
                !FILTER_KEY_CLIENT_UPDATED_AT_TO.equals(key) &&
                !key.startsWith(FILTER_PREFIX_FIELD);
    }

    public String mapClientFilterKey(String key) {
        return switch (key) {
            case FILTER_KEY_CLIENT_SOURCE -> MAPPED_KEY_SOURCE;
            case FILTER_KEY_CLIENT_CREATED_AT_FROM -> MAPPED_KEY_CREATED_AT_FROM;
            case FILTER_KEY_CLIENT_CREATED_AT_TO -> MAPPED_KEY_CREATED_AT_TO;
            case FILTER_KEY_CLIENT_UPDATED_AT_FROM -> MAPPED_KEY_UPDATED_AT_FROM;
            case FILTER_KEY_CLIENT_UPDATED_AT_TO -> MAPPED_KEY_UPDATED_AT_TO;
            default -> key;
        };
    }
}
