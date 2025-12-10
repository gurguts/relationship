package org.example.clientservice.models.dto.clienttype;

import lombok.Data;

@Data
public class StaticFieldsConfig {
    private StaticFieldConfig company;
    private StaticFieldConfig source;
    private StaticFieldConfig createdAt;
    private StaticFieldConfig updatedAt;
    
    public StaticFieldsConfig() {
        company = new StaticFieldConfig(true, 0, 200, "Компанія");
        source = new StaticFieldConfig(true, 999, 200, "Залучення");
        createdAt = new StaticFieldConfig(false, 1000, 150, "Створено");
        updatedAt = new StaticFieldConfig(false, 1001, 150, "Оновлено");
    }
}

