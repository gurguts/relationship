package org.example.purchaseservice.spec;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseReceiptFilterValueParser {
    
    private final AbstractFilterValueParser abstractParser;
    
    public Long parseLong(@NonNull String value) {
        return abstractParser.parseLong(value);
    }
}
