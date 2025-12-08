package org.example.userservice.restControllers.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.models.dto.account.AccountBalanceDTO;
import org.example.userservice.models.dto.account.AccountCreateDTO;
import org.example.userservice.models.dto.account.AccountDTO;
import org.example.userservice.services.account.AccountService;
import org.example.userservice.services.branch.BranchPermissionService;
import org.example.userservice.utils.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final BranchPermissionService branchPermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<Account> accounts = accountService.getAccountsAccessibleToUser(currentUserId);
        List<AccountDTO> dtos = accounts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountDTO> dtos = accounts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<AccountDTO>> getAccountsByBranchId(@PathVariable Long branchId) {
        List<Account> accounts = accountService.getAccountsByBranchId(branchId);
        List<AccountDTO> dtos = accounts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Account account = accountService.getAccountById(id);

        if (account.getBranchId() != null) {
            if (!branchPermissionService.canView(currentUserId, account.getBranchId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        
        return ResponseEntity.ok(mapToDTO(account));
    }

    @GetMapping("/{id}/balances")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<AccountBalanceDTO>> getAccountBalances(@PathVariable Long id) {
        List<AccountBalance> balances = accountService.getAccountBalances(id);
        List<AccountBalanceDTO> dtos = balances.stream()
                .map(this::mapBalanceToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:balance_edit')")
    public ResponseEntity<AccountDTO> createAccount(@RequestBody AccountCreateDTO dto) {
        Account account = new Account();
        account.setName(dto.getName());
        account.setDescription(dto.getDescription());
        account.setUserId(dto.getUserId());
        account.setBranchId(dto.getBranchId());
        account.setCurrencies(dto.getCurrencies());
        Account created = accountService.createAccount(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:balance_edit')")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @RequestBody AccountCreateDTO dto) {
        Account account = new Account();
        account.setName(dto.getName());
        account.setDescription(dto.getDescription());
        account.setUserId(dto.getUserId());
        account.setBranchId(dto.getBranchId());
        account.setCurrencies(dto.getCurrencies());
        Account updated = accountService.updateAccount(id, account);
        return ResponseEntity.ok(mapToDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    private AccountDTO mapToDTO(Account account) {
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

    private AccountBalanceDTO mapBalanceToDTO(AccountBalance balance) {
        return new AccountBalanceDTO(
                balance.getId(),
                balance.getAccountId(),
                balance.getCurrency(),
                balance.getAmount(),
                balance.getUpdatedAt()
        );
    }
}

