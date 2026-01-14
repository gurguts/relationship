package org.example.clientservice.services.client;

import java.time.format.DateTimeFormatter;
import java.util.Set;

final class ClientImportConstants {

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    static final DateTimeFormatter ISO_DATE_TIME_NO_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static final String HEADER_ID = "ID (опціонально)";
    static final String HEADER_SOURCE = "Залучення (назва)";
    static final String HEADER_CREATED_AT = "Дата створення (yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss або yyyy-MM-dd'T'HH:mm)";
    static final String HEADER_UPDATED_AT = "Дата оновлення (yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss або yyyy-MM-dd'T'HH:mm)";
    static final String HEADER_IS_ACTIVE = "Активний (Так/Ні)";
    static final String HEADER_MULTIPLE_SUFFIX = " (через кому, якщо кілька)";
    static final String EXAMPLE_COMPANY = "Приклад компанії";
    static final String EXAMPLE_TEXT = "Приклад тексту";
    static final String EXAMPLE_NUMBER = "123.45";
    static final String EXAMPLE_DATE = "2024-01-01";
    static final String EXAMPLE_PHONE = "+380123456789";
    static final String EXAMPLE_BOOLEAN = "Так";
    static final String SHEET_NAME = "Clients";
    static final int MIN_ROWS_REQUIRED = 2;
    static final int EXAMPLE_ROW_INDEX = 1;
    static final int HEADER_ROW_INDEX = 0;
    static final boolean DEFAULT_IS_ACTIVE = true;
    static final String COLUMN_ID = "id";
    static final String COLUMN_COMPANY = "company";
    static final String COLUMN_SOURCE = "source";
    static final String COLUMN_CREATED_AT = "createdAt";
    static final String COLUMN_UPDATED_AT = "updatedAt";
    static final String COLUMN_IS_ACTIVE = "isActive";
    static final String FIELD_PREFIX = "field_";
    static final String KEYWORD_SOURCE_UA = "Залучення";
    static final String KEYWORD_CREATED_UA = "створення";
    static final String KEYWORD_UPDATED_UA = "оновлення";
    static final String KEYWORD_ACTIVE_UA = "Активний";
    static final String KEYWORD_CREATED_EN = "Created";
    static final String KEYWORD_UPDATED_EN = "Updated";
    static final String KEYWORD_ACTIVE_EN = "Active";
    static final String BOOLEAN_TRUE_UA = "Так";
    static final String BOOLEAN_FALSE_UA = "Ні";
    static final String BOOLEAN_TRUE_EN = "true";
    static final String BOOLEAN_FALSE_EN = "false";
    static final String BOOLEAN_TRUE_NUM = "1";
    static final String BOOLEAN_FALSE_NUM = "0";
    static final String CONTENT_TYPE_EXCEL_OLD = "application/vnd.ms-excel";
    static final String CONTENT_TYPE_EXCEL_NEW = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String FILE_EXTENSION_XLSX = ".xlsx";
    static final String FILE_EXTENSION_XLS = ".xls";
    static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    static final String COMMA_SEPARATOR = ",";
    static final String EMPTY_STRING = "";

    static final Set<String> BOOLEAN_TRUE_VALUES = Set.of(
            BOOLEAN_TRUE_UA.toLowerCase(), BOOLEAN_TRUE_EN.toLowerCase(), BOOLEAN_TRUE_NUM);
    static final Set<String> BOOLEAN_FALSE_VALUES = Set.of(
            BOOLEAN_FALSE_UA.toLowerCase(), BOOLEAN_FALSE_EN.toLowerCase(), BOOLEAN_FALSE_NUM);

    private ClientImportConstants() {
    }
}
