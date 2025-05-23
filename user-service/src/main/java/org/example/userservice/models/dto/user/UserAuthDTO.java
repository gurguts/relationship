package org.example.userservice.models.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UserAuthDTO {
    private Long id;
    private String login;
    private String fullName;
    private BigDecimal balance;
    private List<String> authorities;
    private String role;
}
