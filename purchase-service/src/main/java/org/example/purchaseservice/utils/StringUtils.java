package org.example.purchaseservice.utils;

import lombok.NonNull;

public final class StringUtils {

    private StringUtils() {
    }

    public static String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

