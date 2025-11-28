package org.example.clientservice.repositories.clienttype;

import org.example.clientservice.models.clienttype.ClientTypeField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientTypeFieldRepository extends JpaRepository<ClientTypeField, Long> {
    List<ClientTypeField> findByClientTypeIdOrderByDisplayOrderAsc(Long clientTypeId);
    
    Optional<ClientTypeField> findByClientTypeIdAndFieldName(Long clientTypeId, String fieldName);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isVisibleInTable = true ORDER BY f.displayOrder ASC")
    List<ClientTypeField> findVisibleFieldsByClientTypeId(Long clientTypeId);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isSearchable = true ORDER BY f.displayOrder ASC")
    List<ClientTypeField> findSearchableFieldsByClientTypeId(Long clientTypeId);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isFilterable = true ORDER BY f.displayOrder ASC")
    List<ClientTypeField> findFilterableFieldsByClientTypeId(Long clientTypeId);
    
    @Query("SELECT f FROM ClientTypeField f WHERE f.clientType.id = :clientTypeId AND f.isVisibleInCreate = true ORDER BY f.displayOrder ASC")
    List<ClientTypeField> findVisibleInCreateFieldsByClientTypeId(Long clientTypeId);
}

