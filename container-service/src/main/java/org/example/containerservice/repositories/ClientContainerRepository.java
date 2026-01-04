package org.example.containerservice.repositories;

import lombok.NonNull;
import org.example.containerservice.models.ClientContainer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ClientContainerRepository extends JpaRepository<ClientContainer, Long>,
        JpaSpecificationExecutor<ClientContainer> {
    Optional<ClientContainer> findByClientAndUserAndContainerId(@NonNull Long clientId, @NonNull Long userId, @NonNull Long containerId);

    @EntityGraph(attributePaths = {"container"})
    @NonNull
    List<ClientContainer> findByClient(@NonNull Long clientId);

    @EntityGraph(attributePaths = {"container"})
    @NonNull
    List<ClientContainer> findByClientAndContainerIdOrderByUpdatedAtAsc(@NonNull Long clientId, @NonNull Long containerId);

    @EntityGraph(attributePaths = {"container"})
    @NonNull
    Page<ClientContainer> findAll(Specification<ClientContainer> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"container"})
    @NonNull
    List<ClientContainer> findAll(Specification<ClientContainer> spec, @NonNull Sort sort);
}
