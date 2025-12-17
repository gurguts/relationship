package org.example.apigateway.models.dto;

import java.util.List;

public record UserDTO(String login, List<String> authorities) {
}
