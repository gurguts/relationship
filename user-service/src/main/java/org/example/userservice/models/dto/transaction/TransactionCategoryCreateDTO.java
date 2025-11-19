package org.example.userservice.models.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.userservice.models.transaction.TransactionType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCategoryCreateDTO {
    private TransactionType type;
    private String name;
    private String description;
    private Boolean isActive;
}

