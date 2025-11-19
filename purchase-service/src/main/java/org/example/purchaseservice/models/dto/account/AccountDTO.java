package org.example.purchaseservice.models.dto.account;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AccountDTO {
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private Long branchId;
    private Set<String> currencies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

