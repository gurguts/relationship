package org.example.purchaseservice.models.dto;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, String> details) {
}