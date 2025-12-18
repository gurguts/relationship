package org.example.containerservice.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.List;

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
}

