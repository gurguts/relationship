package org.example.containerservice.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FilterUtils {

    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final String FILTER_CLIENT_SOURCE = "clientSource";
    private static final String FILTER_CLIENT_CREATED_AT_FROM = "clientCreatedAtFrom";
    private static final String FILTER_CLIENT_CREATED_AT_TO = "clientCreatedAtTo";
    private static final String FILTER_CLIENT_UPDATED_AT_FROM = "clientUpdatedAtFrom";
    private static final String FILTER_CLIENT_UPDATED_AT_TO = "clientUpdatedAtTo";
    private static final String FILTER_PREFIX_FIELD = "field";
    
    private static final String TRANSFORMED_SOURCE = "source";
    private static final String TRANSFORMED_CREATED_AT_FROM = "createdAtFrom";
    private static final String TRANSFORMED_CREATED_AT_TO = "createdAtTo";
    private static final String TRANSFORMED_UPDATED_AT_FROM = "updatedAtFrom";
    private static final String TRANSFORMED_UPDATED_AT_TO = "updatedAtTo";
    
    private static final Set<String> COMMON_CLIENT_KEYS = Set.of(
            FILTER_CLIENT_SOURCE,
            FILTER_CLIENT_CREATED_AT_FROM, FILTER_CLIENT_CREATED_AT_TO,
            FILTER_CLIENT_UPDATED_AT_FROM, FILTER_CLIENT_UPDATED_AT_TO
    );

    private FilterUtils() {
    }

    public static Long extractClientTypeId(Map<String, List<String>> filterParams) {
        if (filterParams == null || !filterParams.containsKey(FILTER_KEY_CLIENT_TYPE_ID)) {
            return null;
        }

        List<String> clientTypeIdList = filterParams.get(FILTER_KEY_CLIENT_TYPE_ID);
        if (clientTypeIdList == null || clientTypeIdList.isEmpty()) {
            return null;
        }

        try {
            String firstValue = clientTypeIdList.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> !s.trim().isEmpty())
                    .findFirst()
                    .orElse(null);
            
            if (firstValue == null) {
                return null;
            }
            
            return Long.parseLong(firstValue.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid clientTypeId format in filterParams: {}", clientTypeIdList);
            return null;
        }
    }

    public static Map<String, List<String>> filterClientParams(@NonNull Map<String, List<String>> filterParams, 
                                                                 boolean includeStatusBusinessRouteRegion) {
        if (filterParams.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return filterParams.entrySet().stream()
                .filter(entry -> isClientFilterKey(entry.getKey(), includeStatusBusinessRouteRegion))
                .collect(Collectors.toMap(
                        FilterUtils::transformClientKey,
                        Map.Entry::getValue
                ));
    }
    
    private static boolean isClientFilterKey(String key, boolean includeStatusBusinessRouteRegion) {
        if (key == null) {
            return false;
        }
        
        if (key.startsWith(FILTER_PREFIX_FIELD)) {
            return true;
        }
        
        return COMMON_CLIENT_KEYS.contains(key);
    }
    
    private static String transformClientKey(@NonNull Map.Entry<String, List<String>> entry) {
        String key = entry.getKey();
        return switch (key) {
            case FILTER_CLIENT_SOURCE -> TRANSFORMED_SOURCE;
            case FILTER_CLIENT_CREATED_AT_FROM -> TRANSFORMED_CREATED_AT_FROM;
            case FILTER_CLIENT_CREATED_AT_TO -> TRANSFORMED_CREATED_AT_TO;
            case FILTER_CLIENT_UPDATED_AT_FROM -> TRANSFORMED_UPDATED_AT_FROM;
            case FILTER_CLIENT_UPDATED_AT_TO -> TRANSFORMED_UPDATED_AT_TO;
            default -> key;
        };
    }
}
