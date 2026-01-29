package org.example.purchaseservice.spec;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AbstractFilterValueParser {
    
    public Long parseLong(@NonNull String value) {
        try {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            log.error("Error parsing Long value: {}", value, e);
            return null;
        }
    }
    
    public Long tryParseLong(@NonNull String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
