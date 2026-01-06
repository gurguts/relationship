package org.example.purchaseservice.restControllers.exchange;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.ExchangeRate;
import org.example.purchaseservice.models.dto.ExchangeRateDTO;
import org.example.purchaseservice.models.dto.ExchangeRateUpdateDTO;
import org.example.purchaseservice.mappers.ExchangeRateMapper;
import org.example.purchaseservice.services.exchange.IExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Validated
public class ExchangeRateController {
    
    private final IExchangeRateService exchangeRateService;
    private final ExchangeRateMapper exchangeRateMapper;

    @GetMapping
    public ResponseEntity<List<ExchangeRateDTO>> getAllExchangeRates() {
        List<ExchangeRate> rates = exchangeRateService.getAllExchangeRates();
        List<ExchangeRateDTO> dtos = rates.stream()
                .map(exchangeRateMapper::exchangeRateToExchangeRateDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{currency}/rate")
    public ResponseEntity<java.math.BigDecimal> getExchangeRateToEur(@PathVariable @NotBlank String currency) {
        java.math.BigDecimal rate = exchangeRateService.getExchangeRateToEur(currency);
        return ResponseEntity.ok(rate);
    }
    
    @PreAuthorize("hasAuthority('settings_exchange:edit')")
    @PatchMapping("/{currency}")
    public ResponseEntity<ExchangeRateDTO> updateExchangeRate(
            @PathVariable @NotBlank String currency,
            @RequestBody @Valid @NonNull ExchangeRateUpdateDTO updateDTO) {
        
        ExchangeRate updated = exchangeRateService.updateExchangeRate(currency, updateDTO.getRate());
        return ResponseEntity.ok(exchangeRateMapper.exchangeRateToExchangeRateDTO(updated));
    }
}
