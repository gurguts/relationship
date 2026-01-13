package org.example.userservice.restControllers.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.user.UserException;
import org.example.userservice.mappers.AccountMapper;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.account.AccountBalance;
import org.example.userservice.models.dto.account.AccountBalanceDTO;
import org.example.userservice.models.dto.account.AccountCreateDTO;
import org.example.userservice.models.dto.account.AccountDTO;
import org.example.userservice.services.account.AccountService;
import org.example.userservice.services.branch.BranchPermissionService;
import org.example.userservice.utils.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {
    private final AccountService accountService;
    private final BranchPermissionService branchPermissionService;
    private final AccountMapper accountMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UserException("USER_NOT_FOUND", "Current user ID is null");
        }
        List<Account> accounts = accountService.getAccountsAccessibleToUser(currentUserId);
        List<AccountDTO> dtos = accounts.stream()
                .map(accountMapper::accountToAccountDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountDTO>> getAccountsByUserId(@PathVariable @Positive @NonNull Long userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountDTO> dtos = accounts.stream()
                .map(accountMapper::accountToAccountDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<List<AccountDTO>> getAccountsByBranchId(@PathVariable @Positive @NonNull Long branchId) {
        List<Account> accounts = accountService.getAccountsByBranchId(branchId);
        List<AccountDTO> dtos = accounts.stream()
                .map(accountMapper::accountToAccountDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable @Positive @NonNull Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UserException("USER_NOT_FOUND", "Current user ID is null");
        }
        Account account = accountService.getAccountById(id);

        if (account.getBranchId() != null) {
            if (!branchPermissionService.canView(currentUserId, account.getBranchId())) {
                return ResponseEntity.status(FORBIDDEN).build();
            }
        }
        
        AccountDTO dto = accountMapper.accountToAccountDTO(account);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/balances")
    public ResponseEntity<List<AccountBalanceDTO>> getAccountBalances(@PathVariable @Positive @NonNull Long id) {
        List<AccountBalanceDTO> dtos = accountService.getAccountBalances(id).stream()
                .map(accountMapper::accountBalanceToAccountBalanceDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/balances/batch")
    @PreAuthorize("hasAuthority('finance:view')")
    public ResponseEntity<Map<Long, List<AccountBalanceDTO>>> getAccountBalancesBatch(@RequestBody @NonNull List<@Positive Long> accountIds) {
        Map<Long, List<AccountBalance>> balancesMap = accountService.getAccountBalancesBatch(accountIds);
        Map<Long, List<AccountBalanceDTO>> result = balancesMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(accountMapper::accountBalanceToAccountBalanceDTO)
                                .toList()
                ));
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('finance:balance_edit')")
    public ResponseEntity<AccountDTO> createAccount(@RequestBody @Valid @NonNull AccountCreateDTO dto) {
        Account account = accountMapper.accountCreateDTOToAccount(dto);
        Account created = accountService.createAccount(account);
        AccountDTO createdDto = accountMapper.accountToAccountDTO(created);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdDto.getId())
                .toUri();

        return ResponseEntity.created(location).body(createdDto);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:balance_edit')")
    public ResponseEntity<AccountDTO> updateAccount(
            @PathVariable @Positive @NonNull Long id,
            @RequestBody @Valid @NonNull AccountCreateDTO dto) {
        Account account = accountMapper.accountCreateDTOToAccount(dto);
        Account updated = accountService.updateAccount(id, account);
        AccountDTO updatedDto = accountMapper.accountToAccountDTO(updated);
        return ResponseEntity.ok(updatedDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('settings_finance:delete')")
    public ResponseEntity<Void> deleteAccount(@PathVariable @Positive @NonNull Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}

