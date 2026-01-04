package org.example.containerservice.repositories;

import lombok.NonNull;
import org.example.containerservice.models.ContainerBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ContainerBalanceRepository extends JpaRepository<ContainerBalance, Long> {
    Optional<ContainerBalance> findByUserIdAndContainerId(@NonNull Long userId, @NonNull Long containerId);

    @NonNull
    List<ContainerBalance> findByUserId(@NonNull Long userId);

    @Query("SELECT cb FROM ContainerBalance cb WHERE cb.userId IN :userIds AND cb.container.id = :containerId")
    @NonNull
    List<ContainerBalance> findByUserIdInAndContainerId(@NonNull @Param("userIds") Set<Long> userIds, @NonNull @Param("containerId") Long containerId);

    @Query("SELECT cb FROM ContainerBalance cb ORDER BY cb.userId")
    @NonNull
    List<ContainerBalance> findAllOrderedByUserId();
}
