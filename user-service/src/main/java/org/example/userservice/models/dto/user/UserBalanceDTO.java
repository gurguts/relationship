package org.example.userservice.models.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class UserBalanceDTO {
    private Long id;
    private String fullName;
    private String role;
    private Map<String, BigDecimal> balances;
}