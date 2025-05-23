package org.example.containerservice.repositories;

import org.example.containerservice.models.ContainerBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContainerBalanceRepository extends JpaRepository<ContainerBalance, Long> {
    Optional<ContainerBalance> findByUserIdAndContainerId(Long userId, Long containerId);

    List<ContainerBalance> findByUserId(Long userId);
}
