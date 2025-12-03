package org.example.clientservice.models.dto.client;

import lombok.Getter;
import org.example.clientservice.models.field.Source;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class ExternalClientDataCache {
    private final Map<Long, Source> sourceMap;

    public ExternalClientDataCache(List<Source> sources) {
        this.sourceMap = sources.stream().collect(Collectors.toMap(Source::getId, Function.identity()));
    }
}
