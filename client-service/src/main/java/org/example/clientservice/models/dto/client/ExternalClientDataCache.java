package org.example.clientservice.models.dto.client;

import lombok.NonNull;
import org.example.clientservice.models.field.Source;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ExternalClientDataCache(@NonNull Map<Long, Source> sourceMap) {
    
    public static ExternalClientDataCache of(@NonNull List<Source> sources) {
        Map<Long, Source> sourceMap = sources.stream()
                .collect(Collectors.toMap(Source::getId, Function.identity()));
        return new ExternalClientDataCache(sourceMap);
    }
}
