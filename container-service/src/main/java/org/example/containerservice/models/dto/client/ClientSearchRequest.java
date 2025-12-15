package org.example.containerservice.models.dto.client;

import java.util.List;
import java.util.Map;

public record ClientSearchRequest(String query, Map<String, List<String>> filterParams, Long clientTypeId) {
    public ClientSearchRequest(String query, Map<String, List<String>> filterParams) {
        this(query, filterParams, null);
    }
}