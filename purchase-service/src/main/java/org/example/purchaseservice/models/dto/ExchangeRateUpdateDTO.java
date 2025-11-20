package org.example.purchaseservice.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ExchangeRateUpdateDTO {
    @NotBlank(message = "Валюта обов'язкова")
    private String fromCurrency;
    
    @NotNull(message = "Курс обов'язковий")
    @DecimalMin(value = "0.000001", message = "Курс повинен бути більше нуля")
    private BigDecimal rate;
}


