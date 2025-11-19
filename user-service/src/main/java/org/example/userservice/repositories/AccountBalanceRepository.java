package org.example.userservice.repositories;

import org.example.userservice.models.account.AccountBalance;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountBalanceRepository extends CrudRepository<AccountBalance, Long> {
    Optional<AccountBalance> findByAccountIdAndCurrency(Long accountId, String currency);
    
    List<AccountBalance> findByAccountId(Long accountId);
    
    @Modifying
    @Query("UPDATE AccountBalance ab SET ab.amount = ab.amount + :amount WHERE ab.accountId = :accountId AND ab.currency = :currency")
    void addAmount(@Param("accountId") Long accountId, @Param("currency") String currency, @Param("amount") BigDecimal amount);
    
    @Modifying
    @Query("UPDATE AccountBalance ab SET ab.amount = ab.amount - :amount WHERE ab.accountId = :accountId AND ab.currency = :currency")
    void subtractAmount(@Param("accountId") Long accountId, @Param("currency") String currency, @Param("amount") BigDecimal amount);
}

