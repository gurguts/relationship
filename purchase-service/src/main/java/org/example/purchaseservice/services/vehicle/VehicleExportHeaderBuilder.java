package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VehicleExportHeaderBuilder {
    
    public List<String> buildHeaderList(@NonNull List<Long> sortedCategoryIds, 
                                        @NonNull Map<Long, String> categoryNameMap) {
        List<String> headers = new ArrayList<>();

        headers.add("Відправник");
        headers.add("Отримувач");
        headers.add("Номер машини");
        headers.add("Дата інвойсу EU");
        headers.add("Інвойс EU");
        headers.add("Кількість товару");
        headers.add("Ціна за тонну інвойсу EU");
        headers.add("Повна ціна інвойсу EU");
        headers.add("Рекламація за т");
        headers.add("Повна рекламація");
        headers.add("Загальний дохід (EUR)");
        headers.add("Сума товарів зі складу (EUR)");
        
        for (Long categoryId : sortedCategoryIds) {
            String categoryName = categoryNameMap.get(categoryId);
            headers.add(categoryName + " (EUR)");
            headers.add(categoryName + " (деталі)");
        }

        headers.add("Загальні витрати (EUR)");
        headers.add("Маржа");
        headers.add("Інвойс UA");
        headers.add("Дата інвойсу UA");
        headers.add("Ціна за тонну інвойсу UA");
        headers.add("Повна ціна інвойсу UA");

        headers.add("ID");
        headers.add("Дата відвантаження");
        headers.add("Опис");
        headers.add("Наше завантаження");
        headers.add("Товар");
        headers.add("Країна призначення");
        headers.add("Місце призначення");
        headers.add("Номер декларації");
        headers.add("Термінал");
        headers.add("Водій (ПІБ)");
        headers.add("EUR1");
        headers.add("FITO");
        headers.add("Дата замитнення");
        headers.add("Дата розмитнення");
        headers.add("Дата вивантаження");
        headers.add("Перевізник (назва)");
        headers.add("Перевізник (адреса)");
        headers.add("Перевізник (телефон)");
        headers.add("Перевізник (код)");
        headers.add("Перевізник (рахунок)");
        
        return headers;
    }
}
