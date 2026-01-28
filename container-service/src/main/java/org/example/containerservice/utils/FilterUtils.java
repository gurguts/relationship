package org.example.containerservice.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.services.ClientFilterMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class FilterUtils {

    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";
    
    private static final ClientFilterMapper clientFilterMapper = new ClientFilterMapper();

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

    public static Map<String, List<String>> filterClientParams(@NonNull Map<String, List<String>> filterParams) {
        return clientFilterMapper.mapClientFilterParams(filterParams);
    }
}
