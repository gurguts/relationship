package org.example.userservice.models.dto.transaction;

import lombok.Data;
import org.example.userservice.models.transaction.CounterpartyType;

@Data
public class CounterpartyCreateDTO {
    private CounterpartyType type;
    private String name;
    private String description;
}

