package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CounterpartyRepository extends CrudRepository<Counterparty, Long> {
    @NonNull
    List<Counterparty> findByTypeOrderByNameAsc(@NonNull CounterpartyType type);
    
    Optional<Counterparty> findByTypeAndName(@NonNull CounterpartyType type, @NonNull String name);
    
    boolean existsByTypeAndName(@NonNull CounterpartyType type, @NonNull String name);
}

