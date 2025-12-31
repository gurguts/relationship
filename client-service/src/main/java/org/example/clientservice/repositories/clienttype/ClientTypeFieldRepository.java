package org.example.clientservice.repositories.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypeField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientTypeFieldRepository extends JpaRepository<ClientTypeField, Long> {
    @NonNull
    List<ClientTypeField> findByClientTypeIdOrderByDisplayOrderAsc(@NonNull Long clientTypeId);
    
    Optional<ClientTypeField> findByClientTypeIdAndFieldName(@NonNull Long clientTypeId, @NonNull String fieldName);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isVisibleInTable = true ORDER BY f.displayOrder ASC")
    @NonNull
    List<ClientTypeField> findVisibleFieldsByClientTypeId(@NonNull @Param("clientTypeId") Long clientTypeId);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isSearchable = true ORDER BY f.displayOrder ASC")
    @NonNull
    List<ClientTypeField> findSearchableFieldsByClientTypeId(@NonNull @Param("clientTypeId") Long clientTypeId);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isFilterable = true ORDER BY f.displayOrder ASC")
    @NonNull
    List<ClientTypeField> findFilterableFieldsByClientTypeId(@NonNull @Param("clientTypeId") Long clientTypeId);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isVisibleInCreate = true ORDER BY f.displayOrder ASC")
    @NonNull
    List<ClientTypeField> findVisibleInCreateFieldsByClientTypeId(@NonNull @Param("clientTypeId") Long clientTypeId);
}

