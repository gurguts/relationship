package org.example.purchaseservice.spec;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.models.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurchaseFilterValueParser {
    
    private final AbstractFilterValueParser abstractParser;
    
    public Long parseLong(@NonNull String value) {
        return abstractParser.parseLong(value);
    }
    
    public Long tryParseLong(@NonNull String value) {
        return abstractParser.tryParseLong(value);
    }
    
    public PaymentMethod mapToPaymentMethod(@NonNull String value) {
        return switch (value.trim()) {
            case "2" -> PaymentMethod.CASH;
            case "1" -> PaymentMethod.BANKTRANSFER;
            default -> null;
        };
    }
}
