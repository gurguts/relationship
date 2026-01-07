package org.example.userservice.mappers;

import lombok.NonNull;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.models.dto.account.AccountBalanceDTO;
import org.example.userservice.models.dto.account.AccountCreateDTO;
import org.example.userservice.models.dto.account.AccountDTO;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountDTO accountToAccountDTO(@NonNull Account account) {
        return new AccountDTO(
                account.getId(),
                account.getName(),
                account.getDescription(),
                account.getUserId(),
                account.getBranchId(),
                account.getCurrencies(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public Account accountCreateDTOToAccount(@NonNull AccountCreateDTO dto) {
        Account account = new Account();
        account.setName(dto.getName());
        account.setDescription(dto.getDescription());
        account.setUserId(dto.getUserId());
        account.setBranchId(dto.getBranchId());
        account.setCurrencies(dto.getCurrencies());
        return account;
    }

    public AccountBalanceDTO accountBalanceToAccountBalanceDTO(@NonNull AccountBalance balance) {
        return new AccountBalanceDTO(
                balance.getId(),
                balance.getAccountId(),
                balance.getCurrency(),
                balance.getAmount(),
                balance.getUpdatedAt()
        );
    }
}

