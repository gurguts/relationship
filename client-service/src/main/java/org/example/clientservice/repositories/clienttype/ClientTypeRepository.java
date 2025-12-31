package org.example.clientservice.repositories.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientTypeRepository extends JpaRepository<ClientType, Long> {
    Optional<ClientType> findByName(@NonNull String name);

    @NonNull
    Page<ClientType> findByIsActiveTrue(@NonNull Pageable pageable);
    
    @Query("SELECT ct FROM ClientType ct WHERE ct.isActive = true ORDER BY ct.name")
    @NonNull
    List<ClientType> findAllActiveOrderedByName();
    
    @EntityGraph(attributePaths = {"fields"})
    @Query("SELECT ct FROM ClientType ct WHERE ct.id = :id")
    Optional<ClientType> findByIdWithFields(@NonNull @Param("id") Long id);
}

