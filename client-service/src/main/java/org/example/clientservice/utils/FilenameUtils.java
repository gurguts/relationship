package org.example.clientservice.utils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public final class FilenameUtils {
    private static final int ASCII_BOUNDARY = 128;
    private static final char CHAR_ZERO = '0';
    private static final char CHAR_NINE = '9';
    private static final char CHAR_UPPER_A = 'A';
    private static final char CHAR_UPPER_Z = 'Z';
    private static final char CHAR_LOWER_A = 'a';
    private static final char CHAR_LOWER_Z = 'z';
    private static final char CHAR_HYPHEN = '-';
    private static final char CHAR_UNDERSCORE = '_';
    private static final char CHAR_DOT = '.';
    private static final char CHAR_TILDE = '~';
    private static final char CHAR_REPLACEMENT = '_';
    
    private static final String REGEX_INVALID_CHARS = "[\\\\/:*?\"<>|\\s]+";
    private static final String REGEX_MULTIPLE_UNDERSCORES = "_{2,}";
    private static final String REGEX_LEADING_TRAILING_UNDERSCORE = "^_|_$";
    private static final String REPLACEMENT_UNDERSCORE = "_";
    private static final String EMPTY_STRING = "";
    
    private static final String PERCENT_ENCODING_FORMAT = "%%%02X";

    private FilenameUtils() {
    }

    public static String sanitizeFilename(@NonNull String filename) {
        return filename.replaceAll(REGEX_INVALID_CHARS, REPLACEMENT_UNDERSCORE)
                .replaceAll(REGEX_MULTIPLE_UNDERSCORES, REPLACEMENT_UNDERSCORE)
                .replaceAll(REGEX_LEADING_TRAILING_UNDERSCORE, EMPTY_STRING);
    }

    public static String sanitizeToAscii(@NonNull String filename) {
        StringBuilder result = new StringBuilder();
        for (char c : filename.toCharArray()) {
            if (c < ASCII_BOUNDARY) {
                if (isAllowedChar(c)) {
                    result.append(c);
                } else {
                    result.append(CHAR_REPLACEMENT);
                }
            } else {
                String transliterated = transliterateCyrillic(c);
                if (transliterated.isEmpty() || transliterated.equals(REPLACEMENT_UNDERSCORE)) {
                    result.append(CHAR_REPLACEMENT);
                } else {
                    result.append(transliterated);
                }
            }
        }

        return result.toString().replaceAll(REGEX_MULTIPLE_UNDERSCORES, REPLACEMENT_UNDERSCORE);
    }

    public static String encodeFilenameForRfc5987(@NonNull String filename) {
        try {
            StringBuilder encoded = new StringBuilder();
            byte[] bytes = filename.getBytes(StandardCharsets.UTF_8);
            for (byte b : bytes) {
                if (isAllowedByte(b)) {
                    encoded.append((char) b);
                } else {
                    encoded.append(String.format(PERCENT_ENCODING_FORMAT, b & 0xFF));
                }
            }
            return encoded.toString();
        } catch (Exception e) {
            log.error("Error encoding filename for RFC5987: {}", e.getMessage(), e);
            return filename;
        }
    }

    private static boolean isAllowedChar(char c) {
        return (c >= CHAR_ZERO && c <= CHAR_NINE) ||
                (c >= CHAR_UPPER_A && c <= CHAR_UPPER_Z) ||
                (c >= CHAR_LOWER_A && c <= CHAR_LOWER_Z) ||
                c == CHAR_HYPHEN || c == CHAR_UNDERSCORE || c == CHAR_DOT;
    }

    private static boolean isAllowedByte(byte b) {
        return (b >= CHAR_ZERO && b <= CHAR_NINE) ||
                (b >= CHAR_UPPER_A && b <= CHAR_UPPER_Z) ||
                (b >= CHAR_LOWER_A && b <= CHAR_LOWER_Z) ||
                b == CHAR_HYPHEN || b == CHAR_UNDERSCORE || b == CHAR_DOT || b == CHAR_TILDE;
    }

    private static String transliterateCyrillic(char c) {
        return switch (c) {
            case 'а', 'А' -> "a";
            case 'б', 'Б' -> "b";
            case 'в', 'В' -> "v";
            case 'г', 'Г' -> "g";
            case 'д', 'Д' -> "d";
            case 'е', 'Е' -> "e";
            case 'є', 'Є' -> "ye";
            case 'ж', 'Ж' -> "zh";
            case 'з', 'З' -> "z";
            case 'и', 'И' -> "y";
            case 'і', 'І' -> "i";
            case 'ї', 'Ї' -> "yi";
            case 'й', 'Й' -> "y";
            case 'к', 'К' -> "k";
            case 'л', 'Л' -> "l";
            case 'м', 'М' -> "m";
            case 'н', 'Н' -> "n";
            case 'о', 'О' -> "o";
            case 'п', 'П' -> "p";
            case 'р', 'Р' -> "r";
            case 'с', 'С' -> "s";
            case 'т', 'Т' -> "t";
            case 'у', 'У' -> "u";
            case 'ф', 'Ф' -> "f";
            case 'х', 'Х' -> "kh";
            case 'ц', 'Ц' -> "ts";
            case 'ч', 'Ч' -> "ch";
            case 'ш', 'Ш' -> "sh";
            case 'щ', 'Щ' -> "shch";
            case 'ь', 'Ь' -> EMPTY_STRING;
            case 'ю', 'Ю' -> "yu";
            case 'я', 'Я' -> "ya";
            case 'ъ', 'Ъ' -> EMPTY_STRING;
            default -> REPLACEMENT_UNDERSCORE;
        };
    }
}
