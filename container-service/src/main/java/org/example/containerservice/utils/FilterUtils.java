package org.example.containerservice.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public class FilterUtils {

    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";

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
            return Long.parseLong(clientTypeIdList.getFirst().trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid clientTypeId format in filterParams: {}", clientTypeIdList);
            return null;
        }
    }
}
