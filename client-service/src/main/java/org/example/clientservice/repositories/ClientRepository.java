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
    void deactivateClientById(@Param("clientId") Long clientId);

    @NonNull
    Page<Client> findAll(Specification<Client> spec, @NonNull Pageable pageable);

    @NonNull
    List<Client> findAll(Specification<Client> spec);

    @Modifying
    @Query("UPDATE Client c SET c.urgently = CASE WHEN c.urgently = TRUE THEN FALSE ELSE TRUE END WHERE c.id = :id")
    void toggleUrgently(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Client c SET c.urgently = FALSE, c.route = :routeId WHERE c.id = :id")
    void setFalseUrgentlyAndUpdateRoute(@Param("id") Long id, @Param("routeId") Long routeId);
}
