package org.example.containerservice.repositories;

import org.example.containerservice.models.ContainerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerTransactionRepository extends JpaRepository<ContainerTransaction, Long> {
}
