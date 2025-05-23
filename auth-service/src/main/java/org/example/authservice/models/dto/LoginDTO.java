package org.example.authservice.models.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String login;
    private String password;
}
