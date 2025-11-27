package org.example.userservice.models.dto.user;

import lombok.Data;

import java.util.List;

@Data
public class UserPageDTO {
    private Long id;
    private String login;
    private String fullName;
    private List<String> authorities;
    private String role;
    private String status;
}
