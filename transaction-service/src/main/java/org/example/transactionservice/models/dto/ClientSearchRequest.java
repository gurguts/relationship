package org.example.transactionservice.models.dto;

import java.util.List;
import java.util.Map;

public record ClientSearchRequest(String query, Map<String, List<String>> filterParams) {
}