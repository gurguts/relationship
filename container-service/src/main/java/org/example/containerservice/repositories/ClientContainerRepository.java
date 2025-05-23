package org.example.containerservice.repositories;

import org.example.containerservice.models.ClientContainer;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ClientContainerRepository extends JpaRepository<ClientContainer, Long>,
        JpaSpecificationExecutor<ClientContainer> {
    Optional<ClientContainer> findByClientAndUserAndContainerId(Long clientId, Long userId, Long containerId);

    List<ClientContainer> findByClient(Long clientId);

    List<ClientContainer> findByClientAndContainerIdOrderByUpdatedAtAsc(Long clientId, Long containerId);

    @NotNull
    Page<ClientContainer> findAll(Specification<ClientContainer> spec, @NotNull Pageable pageable);
}
