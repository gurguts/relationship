package org.example.clientservice.repositories.clienttype;

import org.example.clientservice.models.clienttype.ClientFieldValue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientFieldValueRepository extends JpaRepository<ClientFieldValue, Long> {
    List<ClientFieldValue> findByClientIdOrderByDisplayOrderAsc(Long clientId);
    
    List<ClientFieldValue> findByClientIdAndFieldId(Long clientId, Long fieldId);
    
    Optional<ClientFieldValue> findByClientIdAndFieldIdAndDisplayOrder(Long clientId, Long fieldId, Integer displayOrder);
    
    void deleteByClientId(Long clientId);
    
    void deleteByClientIdAndFieldId(Long clientId, Long fieldId);
    
    @Query("SELECT cfv FROM ClientFieldValue cfv WHERE cfv.field.id = :fieldId AND cfv.valueText LIKE %:searchValue%")
    List<ClientFieldValue> findByFieldIdAndValueTextContaining(@Param("fieldId") Long fieldId, @Param("searchValue") String searchValue);
    
    @Query("SELECT cfv FROM ClientFieldValue cfv WHERE cfv.field.id = :fieldId AND cfv.valueNumber BETWEEN :minValue AND :maxValue")
    List<ClientFieldValue> findByFieldIdAndValueNumberBetween(@Param("fieldId") Long fieldId, @Param("minValue") BigDecimal minValue, @Param("maxValue") BigDecimal maxValue);
    
    @Query("SELECT cfv FROM ClientFieldValue cfv WHERE cfv.field.id = :fieldId AND cfv.valueDate BETWEEN :startDate AND :endDate")
    List<ClientFieldValue> findByFieldIdAndValueDateBetween(@Param("fieldId") Long fieldId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT cfv FROM ClientFieldValue cfv WHERE cfv.field.id = :fieldId AND cfv.valueList.id IN :listValueIds")
    List<ClientFieldValue> findByFieldIdAndValueListIdIn(@Param("fieldId") Long fieldId, @Param("listValueIds") List<Long> listValueIds);
    
    @EntityGraph(attributePaths = {"field", "field.listValues", "valueList"})
    @Query("SELECT cfv FROM ClientFieldValue cfv WHERE cfv.client.id IN :clientIds ORDER BY cfv.client.id ASC, cfv.displayOrder ASC")
    List<ClientFieldValue> findByClientIdInWithFieldAndValueList(@Param("clientIds") List<Long> clientIds);
}

