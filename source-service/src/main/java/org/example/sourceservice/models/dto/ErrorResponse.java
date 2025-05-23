package org.example.sourceservice.models.dto;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, String> details) {
}