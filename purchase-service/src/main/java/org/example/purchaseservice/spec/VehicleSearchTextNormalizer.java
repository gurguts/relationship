package org.example.purchaseservice.spec;

import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class VehicleSearchTextNormalizer {

    private static final Map<Character, Character> LATIN_TO_CYRILLIC = Map.ofEntries(
            Map.entry('a', 'а'),
            Map.entry('b', 'в'),
            Map.entry('c', 'с'),
            Map.entry('e', 'е'),
            Map.entry('h', 'н'),
            Map.entry('i', 'і'),
            Map.entry('k', 'к'),
            Map.entry('m', 'м'),
            Map.entry('o', 'о'),
            Map.entry('p', 'р'),
            Map.entry('t', 'т'),
            Map.entry('x', 'х'),
            Map.entry('y', 'у')
    );

    private static final Map<Character, Character> CYRILLIC_TO_LATIN = Map.ofEntries(
            Map.entry('а', 'a'),
            Map.entry('в', 'b'),
            Map.entry('с', 'c'),
            Map.entry('е', 'e'),
            Map.entry('н', 'h'),
            Map.entry('і', 'i'),
            Map.entry('к', 'k'),
            Map.entry('м', 'm'),
            Map.entry('о', 'o'),
            Map.entry('р', 'p'),
            Map.entry('т', 't'),
            Map.entry('х', 'x'),
            Map.entry('у', 'y')
    );

    private VehicleSearchTextNormalizer() {
    }

    public static Set<String> buildSearchVariants(String query) {
        if (!StringUtils.hasText(query)) {
            return Set.of();
        }

        String trimmed = query.trim();
        String lowered = trimmed.toLowerCase(Locale.ROOT);

        Set<String> variants = new LinkedHashSet<>();
        variants.add(lowered);
        variants.add(swap(lowered, LATIN_TO_CYRILLIC));
        variants.add(swap(lowered, CYRILLIC_TO_LATIN));
        return variants;
    }

    private static String swap(String value, Map<Character, Character> map) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            builder.append(map.getOrDefault(current, current));
        }
        return builder.toString();
    }
}
