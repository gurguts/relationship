package org.example.userservice.repositories;

import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.CounterpartyType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CounterpartyRepository extends CrudRepository<Counterparty, Long> {
    List<Counterparty> findByTypeOrderByNameAsc(CounterpartyType type);
    
    Optional<Counterparty> findByTypeAndName(CounterpartyType type, String name);
    
    boolean existsByTypeAndName(CounterpartyType type, String name);
}

