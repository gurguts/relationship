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
        List<String> headerList = new ArrayList<>();
        headerList.add("Сума товарів зі складу (EUR)");
        headerList.add("Сума витрат на машину (EUR)");
        headerList.add("Товар");
        headerList.add("Кількість товару");
        headerList.add("Інвойс UA");
        headerList.add("Дата інвойсу UA");
        headerList.add("Ціна за тонну інвойсу UA");
        headerList.add("Повна ціна інвойсу UA");
        headerList.add("Інвойс EU");
        headerList.add("Дата інвойсу EU");
        headerList.add("Ціна за тонну інвойсу EU");
        headerList.add("Повна ціна інвойсу EU");
        headerList.add("Рекламація за т");
        headerList.add("Повна рекламація");
        headerList.add("Загальні витрати (EUR)");
        headerList.add("Загальний дохід (EUR)");
        headerList.add("Маржа");
        headerList.add("Товари зі складу");
        
        for (Long categoryId : sortedCategoryIds) {
            String categoryName = categoryNameMap.get(categoryId);
            headerList.add(categoryName + " (EUR)");
            headerList.add(categoryName + " (деталі)");
        }
        
        headerList.add("ID");
        headerList.add("Дата відвантаження");
        headerList.add("Номер машини");
        headerList.add("Опис");
        headerList.add("Наше завантаження");
        headerList.add("Відправник");
        headerList.add("Отримувач");
        headerList.add("Країна призначення");
        headerList.add("Місце призначення");
        headerList.add("Номер декларації");
        headerList.add("Термінал");
        headerList.add("Водій (ПІБ)");
        headerList.add("EUR1");
        headerList.add("FITO");
        headerList.add("Дата замитнення");
        headerList.add("Дата розмитнення");
        headerList.add("Дата вивантаження");
        headerList.add("Перевізник (назва)");
        headerList.add("Перевізник (адреса)");
        headerList.add("Перевізник (телефон)");
        headerList.add("Перевізник (код)");
        headerList.add("Перевізник (рахунок)");
        
        return headerList;
    }
}
