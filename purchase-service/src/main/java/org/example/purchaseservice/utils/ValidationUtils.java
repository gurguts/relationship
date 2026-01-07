package org.example.purchaseservice.utils;

import lombok.NonNull;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

public final class ValidationUtils {
    
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void validatePage(int page) {
        if (page < 0) {
            throw new PurchaseException("INVALID_PAGE", 
                    String.format("Page number cannot be negative, got: %d", page));
        }
    }

    public static void validatePageSize(int size, int maxPageSize) {
        if (size <= 0) {
            throw new PurchaseException("INVALID_PAGE_SIZE", "Page size must be positive");
        }
        if (size > maxPageSize) {
            throw new PurchaseException("INVALID_PAGE_SIZE",
                    String.format("Page size cannot exceed %d, got: %d", maxPageSize, size));
        }
    }

    public static void validateSortParams(@NonNull String sort, @NonNull String direction) {
        if (sort.trim().isEmpty()) {
            throw new PurchaseException("INVALID_SORT", "Sort parameter cannot be null or empty");
        }
        if (direction.trim().isEmpty()) {
            throw new PurchaseException("INVALID_SORT_DIRECTION", "Sort direction cannot be null or empty");
        }
        try {
            Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new PurchaseException("INVALID_SORT_DIRECTION",
                    String.format("Invalid sort direction: %s. Valid values: ASC, DESC", direction));
        }
    }

    public static void validateFilters(Map<String, List<String>> filters) {
        if (filters == null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : filters.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new PurchaseException("INVALID_FILTER", "Filter keys cannot be null or empty");
            }
            if (entry.getValue() != null) {
                for (String value : entry.getValue()) {
                    if (value == null || value.trim().isEmpty()) {
                        throw new PurchaseException("INVALID_FILTER", "Filter values cannot be null or empty");
                    }
                }
            }
        }
    }
}

