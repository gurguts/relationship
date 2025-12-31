package org.example.clientservice.repositories.clienttype;

import lombok.NonNull;
import org.example.clientservice.models.clienttype.ClientTypePermission;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientTypePermissionRepository extends JpaRepository<ClientTypePermission, Long> {
    Optional<ClientTypePermission> findByUserIdAndClientTypeId(@NonNull Long userId, @NonNull Long clientTypeId);
    
    @EntityGraph(attributePaths = {"clientType"})
    @NonNull
    List<ClientTypePermission> findByUserId(@NonNull Long userId);
    
    @NonNull
    List<ClientTypePermission> findByClientTypeId(@NonNull Long clientTypeId);
    
    @Query("SELECT ctp FROM ClientTypePermission ctp WHERE ctp.userId = :userId AND ctp.clientType.id = :clientTypeId AND ctp.canView = true")
    Optional<ClientTypePermission> findViewPermission(@NonNull @Param("userId") Long userId, @NonNull @Param("clientTypeId") Long clientTypeId);

}

