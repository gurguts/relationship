package org.example.userservice.spec;

import lombok.NonNull;
import org.example.userservice.exceptions.transaction.TransactionException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionFilterValueParser {

    private static final String ERROR_CODE_INVALID_DATE_FORMAT = "INVALID_DATE_FORMAT";
    private static final String ERROR_CODE_INVALID_ID_FORMAT = "INVALID_ID_FORMAT";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int END_OF_DAY_HOUR = 23;
    private static final int END_OF_DAY_MINUTE = 59;
    private static final int END_OF_DAY_SECOND = 59;
    private static final int END_OF_DAY_NANO = 999999999;

    private final Map<String, List<String>> filters;

    public TransactionFilterValueParser(Map<String, List<String>> filters) {
        this.filters = filters;
    }

    public List<String> getFilterValues(@NonNull String filterKey) {
        return filters == null ? null : filters.get(filterKey);
    }

    public List<Long> parseLongFilter(@NonNull String filterKey) {
        List<String> values = getFilterValues(filterKey);
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(value -> {
                    try {
                        return Long.parseLong(value.trim());
                    } catch (NumberFormatException e) {
                        throw new TransactionException(ERROR_CODE_INVALID_ID_FORMAT,
                                String.format("Invalid ID format for filter %s: %s", filterKey, value));
                    }
                })
                .collect(Collectors.toList());
    }

    public LocalDateTime parseDateFromFilter(@NonNull String filterKey, boolean isStartOfDay) {
        List<String> dateValues = getFilterValues(filterKey);
        if (dateValues == null || dateValues.isEmpty()) {
            return null;
        }

        String dateString = dateValues.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .findFirst()
                .orElse(null);

        if (dateString == null) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(dateString.trim(), DATE_FORMATTER);
            return isStartOfDay ? date.atStartOfDay() : date.atTime(END_OF_DAY_HOUR, END_OF_DAY_MINUTE, END_OF_DAY_SECOND, END_OF_DAY_NANO);
        } catch (DateTimeParseException e) {
            throw new TransactionException(ERROR_CODE_INVALID_DATE_FORMAT,
                    String.format("Invalid date format for filter %s: %s. Expected format: yyyy-MM-dd", filterKey, dateString));
        }
    }
}
