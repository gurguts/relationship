package org.example.userservice.repositories;

import lombok.NonNull;
import org.example.userservice.models.account.AccountBalance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountBalanceRepository extends CrudRepository<AccountBalance, Long> {
    Optional<AccountBalance> findByAccountIdAndCurrency(@NonNull Long accountId, @NonNull String currency);
    
    @NonNull
    List<AccountBalance> findByAccountId(@NonNull Long accountId);
    
    @NonNull
    List<AccountBalance> findByAccountIdIn(@NonNull List<Long> accountIds);
    
    @Modifying
    @Query("UPDATE AccountBalance ab SET ab.amount = ab.amount + :amount WHERE ab.accountId = :accountId AND ab.currency = :currency")
    void addAmount(@Param("accountId") @NonNull Long accountId, @Param("currency") @NonNull String currency, @Param("amount") @NonNull BigDecimal amount);
    
    @Modifying
    @Query("UPDATE AccountBalance ab SET ab.amount = ab.amount - :amount WHERE ab.accountId = :accountId AND ab.currency = :currency")
    void subtractAmount(@Param("accountId") @NonNull Long accountId, @Param("currency") @NonNull String currency, @Param("amount") @NonNull BigDecimal amount);
    
    @Query("SELECT COUNT(ab) FROM AccountBalance ab WHERE ab.accountId = :accountId AND ab.amount != 0")
    long countNonZeroBalances(@Param("accountId") @NonNull Long accountId);
}

