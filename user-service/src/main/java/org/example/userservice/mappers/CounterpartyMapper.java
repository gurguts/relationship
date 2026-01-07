package org.example.userservice.mappers;

import lombok.NonNull;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.dto.transaction.CounterpartyCreateDTO;
import org.example.userservice.models.dto.transaction.CounterpartyDTO;
import org.springframework.stereotype.Component;

@Component
public class CounterpartyMapper {

    public CounterpartyDTO counterpartyToCounterpartyDTO(@NonNull Counterparty counterparty) {
        return new CounterpartyDTO(
                counterparty.getId(),
                counterparty.getType(),
                counterparty.getName(),
                counterparty.getDescription(),
                counterparty.getCreatedAt(),
                counterparty.getUpdatedAt()
        );
    }

    public Counterparty counterpartyCreateDTOToCounterparty(@NonNull CounterpartyCreateDTO dto) {
        Counterparty counterparty = new Counterparty();
        counterparty.setType(dto.getType());
        counterparty.setName(dto.getName());
        counterparty.setDescription(dto.getDescription());
        return counterparty;
    }
}

