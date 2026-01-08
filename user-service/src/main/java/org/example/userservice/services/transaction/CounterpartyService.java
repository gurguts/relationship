package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.CounterpartyNotFoundException;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;
import org.example.userservice.repositories.CounterpartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounterpartyService {
    private static final String ERROR_CODE_COUNTERPARTY_ALREADY_EXISTS = "COUNTERPARTY_ALREADY_EXISTS";
    private static final String ERROR_CODE_NAME_REQUIRED = "NAME_REQUIRED";
    private static final String ERROR_CODE_TYPE_REQUIRED = "TYPE_REQUIRED";

    private final CounterpartyRepository counterpartyRepository;

    @Transactional(readOnly = true)
    public @NonNull List<Counterparty> getCounterpartiesByType(@NonNull CounterpartyType type) {
        return counterpartyRepository.findByTypeOrderByNameAsc(type);
    }

    @Transactional(readOnly = true)
    public Counterparty getCounterpartyById(@NonNull Long id) {
        return counterpartyRepository.findById(id)
                .orElseThrow(() -> new CounterpartyNotFoundException(
                        String.format("Counterparty with ID %d not found", id)));
    }

    @Transactional
    public Counterparty createCounterparty(@NonNull Counterparty counterparty) {
        validateCounterparty(counterparty);
        
        if (counterpartyRepository.existsByTypeAndName(counterparty.getType(), counterparty.getName())) {
            throw new TransactionException(ERROR_CODE_COUNTERPARTY_ALREADY_EXISTS,
                    String.format("Counterparty '%s' for type '%s' already exists", counterparty.getName(), counterparty.getType()));
        }
        
        Counterparty saved = counterpartyRepository.save(counterparty);
        log.info("Created counterparty: id={}, type={}, name={}", saved.getId(), saved.getType(), saved.getName());
        return saved;
    }

    @Transactional
    public Counterparty updateCounterparty(@NonNull Long id, @NonNull Counterparty updatedCounterparty) {
        validateCounterparty(updatedCounterparty);
        
        Counterparty counterparty = getCounterpartyById(id);
        
        boolean nameChanged = !counterparty.getName().equals(updatedCounterparty.getName());
        boolean typeChanged = !counterparty.getType().equals(updatedCounterparty.getType());
        
        if (nameChanged || typeChanged) {
            if (counterpartyRepository.existsByTypeAndName(updatedCounterparty.getType(), updatedCounterparty.getName())) {
                Counterparty existing = counterpartyRepository.findByTypeAndName(updatedCounterparty.getType(), updatedCounterparty.getName())
                        .orElse(null);
                if (existing != null && !existing.getId().equals(id)) {
                    throw new TransactionException(ERROR_CODE_COUNTERPARTY_ALREADY_EXISTS,
                            String.format("Counterparty '%s' for type '%s' already exists", updatedCounterparty.getName(), updatedCounterparty.getType()));
                }
            }
        }
        
        counterparty.setName(updatedCounterparty.getName());
        counterparty.setDescription(updatedCounterparty.getDescription());
        counterparty.setType(updatedCounterparty.getType());
        
        Counterparty saved = counterpartyRepository.save(counterparty);
        log.info("Updated counterparty: id={}, type={}, name={}", saved.getId(), saved.getType(), saved.getName());
        return saved;
    }

    @Transactional
    public void deleteCounterparty(@NonNull Long id) {
        Counterparty counterparty = getCounterpartyById(id);
        counterpartyRepository.delete(counterparty);
        log.info("Deleted counterparty: id={}, type={}, name={}", counterparty.getId(), counterparty.getType(), counterparty.getName());
    }

    private void validateCounterparty(@NonNull Counterparty counterparty) {
        if (counterparty.getType() == null) {
            throw new TransactionException(ERROR_CODE_TYPE_REQUIRED, "Counterparty type is required");
        }
        if (counterparty.getName() == null || counterparty.getName().trim().isEmpty()) {
            throw new TransactionException(ERROR_CODE_NAME_REQUIRED, "Counterparty name is required");
        }
    }
}
