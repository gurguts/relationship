package org.example.clientservice.repositories.clienttype;

import org.example.clientservice.models.clienttype.ClientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientTypeRepository extends JpaRepository<ClientType, Long> {
    Optional<ClientType> findByName(String name);
    
    List<ClientType> findByIsActiveTrue();
    
    Page<ClientType> findByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT ct FROM ClientType ct WHERE ct.isActive = true ORDER BY ct.name")
    List<ClientType> findAllActiveOrderedByName();
}

