package org.example.purchaseservice.models;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class FilterParams {
    private List<Long> userIds;
    private List<Long> productIds;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    public FilterParams(List<Long> userIds, List<Long> productIds, LocalDate dateFrom, LocalDate dateTo) {
        this.userIds = userIds;
        this.productIds = productIds;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }
}
