package org.example.purchaseservice.restControllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.ExchangeRate;
import org.example.purchaseservice.models.dto.ExchangeRateDTO;
import org.example.purchaseservice.models.dto.ExchangeRateUpdateDTO;
import org.example.purchaseservice.services.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateController {
    
    private final ExchangeRateService exchangeRateService;
    
    @PreAuthorize("hasAuthority('purchase:view')")
    @GetMapping
    public ResponseEntity<List<ExchangeRateDTO>> getAllExchangeRates() {
        List<ExchangeRate> rates = exchangeRateService.getAllExchangeRates();
        List<ExchangeRateDTO> dtos = rates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PreAuthorize("hasAuthority('settings_exchange:edit')")
    @PutMapping("/{currency}")
    public ResponseEntity<ExchangeRateDTO> updateExchangeRate(
            @PathVariable String currency,
            @Valid @RequestBody ExchangeRateUpdateDTO updateDTO) {
        
        ExchangeRate updated = exchangeRateService.updateExchangeRate(currency, updateDTO.getRate());
        return ResponseEntity.ok(toDTO(updated));
    }
    
    private ExchangeRateDTO toDTO(ExchangeRate rate) {
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


