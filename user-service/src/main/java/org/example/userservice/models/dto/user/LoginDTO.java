package org.example.userservice.models.dto.user;

import lombok.Data;

@Data
public class LoginDTO {
    private String login;
    private String password;
}
