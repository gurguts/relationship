package org.example.userservice.models.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDetDTO {
    private String login;
    private String password;
    private List<String> authorities;
}