package org.example.purchaseservice.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class FilterUtils {

    public static Long extractClientTypeId(Map<String, List<String>> filterParams) {
        if (filterParams == null || !filterParams.containsKey("clientTypeId") || filterParams.get("clientTypeId") == null 
                || filterParams.get("clientTypeId").isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(filterParams.get("clientTypeId").get(0));
        } catch (NumberFormatException e) {
            log.warn("Invalid clientTypeId in filterParams: {}", filterParams.get("clientTypeId"));
            return null;
        }
    }

    /**
     * Filter and transform client-related parameters from filterParams.
     * Includes status, business, route, region, clientProduct, clientSource, date ranges, and field filters.
     * Transforms key names (e.g., "clientSource" -> "source").
     */
    public static Map<String, List<String>> filterClientParams(Map<String, List<String>> filterParams, boolean includeStatusBusinessRouteRegion) {
        if (filterParams == null) {
            return Collections.emptyMap();
        }
        
        return filterParams.entrySet().stream()
                .filter(entry -> {
                    String key = entry.getKey();
                    if (includeStatusBusinessRouteRegion) {
                        return key.equals("status") || key.equals("business") ||
                                key.equals("route") || key.equals("region") || key.equals("clientProduct") ||
                                key.equals("clientSource") ||
                                key.equals("clientCreatedAtFrom") || key.equals("clientCreatedAtTo") ||
                                key.equals("clientUpdatedAtFrom") || key.equals("clientUpdatedAtTo") ||
                                key.startsWith("field");
                    } else {
                        return key.equals("clientProduct") ||
                                key.equals("clientSource") ||
                                key.equals("clientCreatedAtFrom") || key.equals("clientCreatedAtTo") ||
                                key.equals("clientUpdatedAtFrom") || key.equals("clientUpdatedAtTo") ||
                                key.startsWith("field");
                    }
                })
                .collect(Collectors.toMap(
                        entry -> {
                            String key = entry.getKey();
                            if (key.equals("clientSource")) return "source";
                            if (key.equals("clientCreatedAtFrom")) return "createdAtFrom";
                            if (key.equals("clientCreatedAtTo")) return "createdAtTo";
                            if (key.equals("clientUpdatedAtFrom")) return "updatedAtFrom";
                            if (key.equals("clientUpdatedAtTo")) return "updatedAtTo";
                            return key;
                        },
                        Map.Entry::getValue
                ));
    }
}

