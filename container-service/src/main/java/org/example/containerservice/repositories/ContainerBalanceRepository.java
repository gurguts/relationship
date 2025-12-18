package org.example.containerservice.repositories;

import org.example.containerservice.models.ContainerBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContainerBalanceRepository extends JpaRepository<ContainerBalance, Long> {
    Optional<ContainerBalance> findByUserIdAndContainerId(Long userId, Long containerId);

    List<ContainerBalance> findByUserId(Long userId);

    @Query("SELECT cb FROM ContainerBalance cb WHERE cb.userId IN :userIds AND cb.container.id = :containerId")
    List<ContainerBalance> findByUserIdInAndContainerId(@Param("userIds") java.util.Set<Long> userIds, @Param("containerId") Long containerId);

    @Query("SELECT cb FROM ContainerBalance cb ORDER BY cb.userId")
    List<ContainerBalance> findAllOrderedByUserId();
}
