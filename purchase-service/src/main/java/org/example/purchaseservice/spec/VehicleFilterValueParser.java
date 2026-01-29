package org.example.purchaseservice.spec;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleFilterValueParser {
    
    private final AbstractFilterValueParser abstractParser;
    
    public Long tryParseLong(@NonNull String value) {
        return abstractParser.tryParseLong(value);
    }
}
