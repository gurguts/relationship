package org.example.productservice.models.dto;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, String> details) {
}