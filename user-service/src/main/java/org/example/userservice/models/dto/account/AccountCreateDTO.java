package org.example.userservice.models.dto.account;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateDTO {
    private String name;
    private String description;
    private Long userId;
    private Long branchId;
    private Set<String> currencies;
}

