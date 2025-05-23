package org.example.apigateway.models.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private String login;

    private List<String> authorities;

    public UserDTO(String login, List<String> authorities) {
        this.login = login;
        this.authorities = authorities;
    }
}
