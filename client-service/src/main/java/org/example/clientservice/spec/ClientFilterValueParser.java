package org.example.clientservice.spec;

import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ClientFilterValueParser {

    private static final int FIELD_PREFIX_LENGTH = 6;
    private static final String LIKE_PATTERN = "%%%s%%";

    public static String escapeQuery(String query) {
        return query.toLowerCase().replace("%", "\\%").replace("_", "\\_");
    }

    public static String createSearchPattern(String query) {
        String escapedQuery = escapeQuery(query);
        return String.format(LIKE_PATTERN, escapedQuery);
    }

    public static Long extractFieldIdFromKey(String key, int suffixLength) {
        try {
            int endIndex = suffixLength > 0 ? key.length() - suffixLength : key.length();
            String fieldIdStr = key.substring(FIELD_PREFIX_LENGTH, endIndex);
            return Long.parseLong(fieldIdStr);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            log.warn("Invalid field ID format in key: {}", key);
            return null;
        }
    }

    public static List<Long> parseSourceIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        try {
            return values.stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ClientException("INVALID_FILTER", 
                String.format("Incorrect ID format in filter source: %s", values));
        }
    }

    public static LocalDateTime parseDateFilter(List<String> values, boolean isFrom) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(values.getFirst().trim());
            return isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new ClientException("INVALID_FILTER", 
                String.format("Incorrect date format in filter: %s", values));
        }
    }

    public static LocalDate parseDate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(values.getFirst().trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
