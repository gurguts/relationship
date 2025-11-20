package org.example.purchaseservice.models.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExchangeRateDTO {
    private Long id;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal rate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long updatedByUserId;
}


