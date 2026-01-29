package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import org.example.purchaseservice.utils.FilterUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PurchaseFilterProcessor {
    
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    private static final String FILTER_KEY_CLIENT_PRODUCT = "clientProduct";
    private static final String FILTER_KEY_CLIENT_SOURCE = "clientSource";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_FROM = "clientCreatedAtFrom";
    private static final String FILTER_KEY_CLIENT_CREATED_AT_TO = "clientCreatedAtTo";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_FROM = "clientUpdatedAtFrom";
    private static final String FILTER_KEY_CLIENT_UPDATED_AT_TO = "clientUpdatedAtTo";
    private static final String FILTER_PREFIX_FIELD = "field";

    private static final Set<String> EXCLUDED_FILTER_KEYS = Set.of(
            FILTER_KEY_CLIENT_TYPE_ID,
            FILTER_KEY_CLIENT_PRODUCT,
            FILTER_KEY_CLIENT_SOURCE,
            FILTER_KEY_CLIENT_CREATED_AT_FROM,
            FILTER_KEY_CLIENT_CREATED_AT_TO,
            FILTER_KEY_CLIENT_UPDATED_AT_FROM,
            FILTER_KEY_CLIENT_UPDATED_AT_TO
    );
    
    public record SearchFilters(
            Long clientTypeId,
            Map<String, List<String>> clientFilterParams,
            Map<String, List<String>> purchaseFilterParams
    ) {}
    
    public SearchFilters extractFilters(Map<String, List<String>> filterParams) {
        Long clientTypeId = FilterUtils.extractClientTypeId(filterParams);
        Map<String, List<String>> clientFilterParams = FilterUtils.filterClientParams(filterParams, false);
        Map<String, List<String>> purchaseFilterParams = filterPurchaseParams(filterParams);
        return new SearchFilters(clientTypeId, clientFilterParams, purchaseFilterParams);
    }
    
    private Map<String, List<String>> filterPurchaseParams(Map<String, List<String>> filterParams) {
        if (filterParams == null || filterParams.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return filterParams.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .filter(entry -> !isExcludedFilterKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    private boolean isExcludedFilterKey(@NonNull String key) {
        return EXCLUDED_FILTER_KEYS.contains(key) || key.startsWith(FILTER_PREFIX_FIELD);
    }
}
