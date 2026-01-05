package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.ExchangeRate;
import org.example.purchaseservice.models.dto.ExchangeRateDTO;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateMapper {

    public ExchangeRateDTO exchangeRateToExchangeRateDTO(@NonNull ExchangeRate rate) {
        ExchangeRateDTO dto = new ExchangeRateDTO();
        dto.setId(rate.getId());
        dto.setFromCurrency(rate.getFromCurrency());
        dto.setToCurrency(rate.getToCurrency());
        dto.setRate(rate.getRate());
        dto.setCreatedAt(rate.getCreatedAt());
        dto.setUpdatedAt(rate.getUpdatedAt());
        dto.setUpdatedByUserId(rate.getUpdatedByUserId());
        return dto;
    }
}