package org.example.userservice.repositories;

import org.example.userservice.models.balance.Balance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BalanceRepository extends CrudRepository<Balance, Long> {
    Optional<Balance> findByUserId(Long userId);

    List<Balance> findByUserIdIn(List<Long> userIds);

    @Modifying
    @Query("DELETE FROM Balance b WHERE b.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
