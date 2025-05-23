package org.example.authservice.models.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.ResourceBundle;

@Data
public class UserAuthDTO {
    private Long id;
    private String login;
    private String fullName;
    private BigDecimal balance;
    private List<String> authorities;
    private String role;

    public String translateRole(){
        ResourceBundle bundle = ResourceBundle.getBundle("messages");
        return bundle.getString("role." + role.toLowerCase());
    }
}
