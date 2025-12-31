package org.example.clientservice.repositories.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClientFieldValueRepository extends JpaRepository<ClientFieldValue, Long> {
    @NonNull
    List<ClientFieldValue> findByClientIdOrderByDisplayOrderAsc(@NonNull Long clientId);

    @EntityGraph(attributePaths = {"field", "field.listValues", "valueList"})
    @Query("SELECT cfv FROM ClientFieldValue cfv WHERE cfv.client.id IN :clientIds ORDER BY cfv.client.id ASC, cfv.displayOrder ASC")
    @NonNull
    List<ClientFieldValue> findByClientIdInWithFieldAndValueList(@NonNull @Param("clientIds") List<Long> clientIds);
}

