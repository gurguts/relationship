package org.example.containerservice.spec;

import org.example.containerservice.exceptions.ContainerException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class ClientContainerFilterValueParser {

    private static final String ERROR_INVALID_FILTER = "INVALID_FILTER";
    private static final String MESSAGE_INCORRECT_ID_FORMAT = "Incorrect ID format in filter %s: %s";
    private static final String MESSAGE_INCORRECT_DATE_FORMAT = "Incorrect date format in filter %s: %s";
    private static final String MESSAGE_INCORRECT_NUMERIC_FORMAT = "Incorrect numeric format in filter %s: %s";

    public static List<Long> parseIds(List<String> values, String filterName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        try {
            return values.stream()
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_ID_FORMAT, filterName, values));
        }
    }

    public static LocalDateTime parseDate(List<String> values, boolean isFrom, String filterName) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(values.getFirst().trim());
            return isFrom ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_DATE_FORMAT, filterName, values));
        }
    }

    public static Double parseNumeric(List<String> values, String filterName) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(values.getFirst().trim());
        } catch (NumberFormatException e) {
            throw new ContainerException(ERROR_INVALID_FILTER,
                    String.format(MESSAGE_INCORRECT_NUMERIC_FORMAT, filterName, values));
        }
    }

    public static String escapeQuery(String query) {
        return query.toLowerCase().replace("%", "\\%").replace("_", "\\_");
    }
}
