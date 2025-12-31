package org.example.clientservice.repositories;

import lombok.NonNull;
import org.example.clientservice.models.client.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {
    @Modifying
    @Query("UPDATE Client c SET c.isActive = false WHERE c.id = :clientId")
    void deactivateClientById(@NonNull @Param("clientId") Long clientId);

    @Modifying
    @Query("UPDATE Client c SET c.isActive = true WHERE c.id = :clientId")
    void activateClientById(@NonNull @Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"clientType"})
    @NonNull
    Page<Client> findAll(Specification<Client> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"fieldValues", "fieldValues.field", "fieldValues.field.listValues", "fieldValues.valueList", "clientType"})
    @NonNull
    List<Client> findAll(Specification<Client> spec);

    @EntityGraph(attributePaths = {"clientType", "fieldValues", "fieldValues.field", "fieldValues.field.listValues", "fieldValues.valueList"})
    @NonNull
    java.util.Optional<Client> findById(@NonNull Long id);

    long countByClientTypeId(@NonNull Long clientTypeId);

}
