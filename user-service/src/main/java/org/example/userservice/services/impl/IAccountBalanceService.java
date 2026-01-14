package org.example.userservice.services.impl;

import lombok.NonNull;

import java.math.BigDecimal;

public interface IAccountBalanceService {
    void getBalance(@NonNull Long accountId, @NonNull String currency);
    
    void addAmount(@NonNull Long accountId, @NonNull String currency, @NonNull BigDecimal amount);
    
    void subtractAmount(@NonNull Long accountId, @NonNull String currency, @NonNull BigDecimal amount);
}
