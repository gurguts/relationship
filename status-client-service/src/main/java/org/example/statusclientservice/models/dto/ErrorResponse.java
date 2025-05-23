package org.example.statusclientservice.models.dto;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, String> details) {
}