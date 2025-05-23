package org.example.balanceservcie.repositories;

import org.example.balanceservcie.models.Balance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BalanceRepository extends CrudRepository<Balance, Long> {
    Optional<Balance> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM Balance b WHERE b.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
