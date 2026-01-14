package org.example.userservice.services.impl;

import lombok.NonNull;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;

import java.util.List;

public interface ICounterpartyService {
    @NonNull List<Counterparty> getCounterpartiesByType(@NonNull CounterpartyType type);
    
    Counterparty getCounterpartyById(@NonNull Long id);
    
    Counterparty createCounterparty(@NonNull Counterparty counterparty);
    
    Counterparty updateCounterparty(@NonNull Long id, @NonNull Counterparty updatedCounterparty);
    
    void deleteCounterparty(@NonNull Long id);
}
