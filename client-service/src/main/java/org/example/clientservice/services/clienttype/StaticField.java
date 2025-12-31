package org.example.clientservice.services.clienttype;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StaticField {
    COMPANY("company", 1, "TEXT", "Компанія"),
    SOURCE("source", 2, "TEXT", "Залучення"),
    CREATED_AT("createdAt", 3, "DATE", "Створено"),
    UPDATED_AT("updatedAt", 4, "DATE", "Оновлено");
    
    private final String fieldName;
    private final int fieldId;
    private final String fieldType;
    private final String defaultLabel;

}

