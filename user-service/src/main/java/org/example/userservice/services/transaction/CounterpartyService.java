package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;
import org.example.userservice.repositories.CounterpartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CounterpartyService {
    private final CounterpartyRepository counterpartyRepository;

    @Transactional(readOnly = true)
    public List<Counterparty> getCounterpartiesByType(CounterpartyType type) {
        return counterpartyRepository.findByTypeOrderByNameAsc(type);
    }

    @Transactional(readOnly = true)
    public Counterparty getCounterpartyById(Long id) {
        return counterpartyRepository.findById(id)
                .orElseThrow(() -> new TransactionException("COUNTERPARTY_NOT_FOUND",
                        String.format("Counterparty with ID %d not found", id)));
    }

    @Transactional
    public Counterparty createCounterparty(Counterparty counterparty) {
        if (counterpartyRepository.existsByTypeAndName(counterparty.getType(), counterparty.getName())) {
            throw new TransactionException("COUNTERPARTY_ALREADY_EXISTS",
                    String.format("Counterparty '%s' for type '%s' already exists", counterparty.getName(), counterparty.getType()));
        }
        return counterpartyRepository.save(counterparty);
    }

    @Transactional
    public Counterparty updateCounterparty(Long id, Counterparty updatedCounterparty) {
        Counterparty counterparty = getCounterpartyById(id);
        counterparty.setName(updatedCounterparty.getName());
        counterparty.setDescription(updatedCounterparty.getDescription());
        return counterpartyRepository.save(counterparty);
    }

    @Transactional
    public void deleteCounterparty(Long id) {
        Counterparty counterparty = getCounterpartyById(id);
        counterpartyRepository.delete(counterparty);
    }
}

