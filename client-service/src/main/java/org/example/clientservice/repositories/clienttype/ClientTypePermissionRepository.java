package org.example.clientservice.repositories.clienttype;

import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientTypePermissionRepository extends JpaRepository<ClientTypePermission, Long> {
    Optional<ClientTypePermission> findByUserIdAndClientTypeId(Long userId, Long clientTypeId);
    
    @EntityGraph(attributePaths = {"clientType"})
    List<ClientTypePermission> findByUserId(Long userId);
    
    List<ClientTypePermission> findByClientTypeId(Long clientTypeId);
    
    @Query("SELECT ctp FROM ClientTypePermission ctp WHERE ctp.userId = :userId AND ctp.clientType.id = :clientTypeId AND ctp.canView = true")
    Optional<ClientTypePermission> findViewPermission(@Param("userId") Long userId, @Param("clientTypeId") Long clientTypeId);
    
    void deleteByUserId(Long userId);
    
    void deleteByClientTypeId(Long clientTypeId);
}

