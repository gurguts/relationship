package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.clients.ClientApiClient;
import org.example.userservice.clients.VehicleApiClient;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.dto.PageResponse;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.repositories.CounterpartyRepository;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionSearchService;
import org.example.userservice.spec.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSearchService implements ITransactionSearchService {
    private static final String ERROR_CODE_INVALID_PAGE = "INVALID_PAGE";
    private static final String ERROR_CODE_INVALID_PAGE_SIZE = "INVALID_PAGE_SIZE";
    private static final String ERROR_CODE_INVALID_SORT_DIRECTION = "INVALID_SORT_DIRECTION";
    private static final int MAX_PAGE_SIZE = 1000;
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ClientApiClient clientApiClient;
    private final VehicleApiClient vehicleApiClient;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final CounterpartyRepository counterpartyRepository;

    @Override
    @NonNull
    @Transactional(readOnly = true)
    public PageResponse<TransactionPageDTO> getTransactionsWithPagination(int page, int size, String sort,
                                                                          String direction, Map<String, List<String>> filters) {
        validateSearchParameters(page, size, sort, direction);
        
        log.info("Searching transactions: page={}, size={}, sort={}, direction={}, filters={}", 
                page, size, sort, direction, filters);

        Sort sortBy = buildSort(sort, direction);
        PageRequest pageRequest = PageRequest.of(page, size, sortBy);

        TransactionSpecification spec = new TransactionSpecification(filters);
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageRequest);

        log.info("Found {} transactions (page {} of {})", 
                transactionPage.getContent().size(), transactionPage.getNumber() + 1, transactionPage.getTotalPages());

        Map<Long, String> clientCompanyMap = loadClientData(transactionPage.getContent());
        Map<Long, String> accountNameMap = loadAccountData(transactionPage.getContent());
        Map<Long, String> categoryNameMap = loadCategoryData(transactionPage.getContent());
        Map<Long, String> counterpartyNameMap = loadCounterpartyData(transactionPage.getContent());
        Map<Long, String> vehicleNumberMap = loadVehicleData(transactionPage.getContent());

        List<TransactionPageDTO> content = mapTransactionsToDTOs(transactionPage.getContent(), 
                clientCompanyMap, accountNameMap, categoryNameMap, counterpartyNameMap, vehicleNumberMap);

        return new PageResponse<>(transactionPage.getNumber(), transactionPage.getSize(),
                transactionPage.getTotalElements(), transactionPage.getTotalPages(), content);
    }

    private void validateSearchParameters(int page, int size, String sort, String direction) {
        if (page < 0) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE, 
                    String.format("Page number cannot be negative. Requested page: %d", page));
        }
        
        if (size <= 0) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE_SIZE, 
                    String.format("Page size must be positive. Requested size: %d", size));
        }
        
        if (size > MAX_PAGE_SIZE) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE_SIZE, 
                    String.format("Page size cannot exceed %d. Requested size: %d", MAX_PAGE_SIZE, size));
        }
        
        if (sort == null || sort.trim().isEmpty()) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE, "Sort field cannot be null or empty");
        }
        
        if (direction != null && !direction.equalsIgnoreCase("ASC") && !direction.equalsIgnoreCase("DESC")) {
            throw new TransactionException(ERROR_CODE_INVALID_SORT_DIRECTION, 
                    String.format("Invalid sort direction: %s. Must be ASC or DESC", direction));
        }
    }

    private Sort buildSort(String sort, String direction) {
        String sortField = sort != null && !sort.trim().isEmpty() ? sort : DEFAULT_SORT_FIELD;
        String sortDirection = direction != null && !direction.trim().isEmpty() ? direction : DEFAULT_SORT_DIRECTION;
        
        try {
            Sort.Direction sortDir = Sort.Direction.fromString(sortDirection);
            return Sort.by(sortDir, sortField);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sort direction: {}, using default: {}", sortDirection, DEFAULT_SORT_DIRECTION);
            return Sort.by(Sort.Direction.fromString(DEFAULT_SORT_DIRECTION), sortField);
        }
    }

    private Map<Long, String> loadClientData(@NonNull List<Transaction> transactions) {
        List<Long> clientIds = transactions.stream()
                .map(Transaction::getClientId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (clientIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return Optional.ofNullable(clientApiClient.getClients(clientIds).getBody())
                    .map(clients -> clients.stream()
                            .flatMap(map -> map.entrySet().stream())
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    Map.Entry::getValue,
                                    (a, _) -> a
                            )))
                    .orElse(Collections.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to load client data: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, String> loadAccountData(@NonNull List<Transaction> transactions) {
        Set<Long> accountIds = transactions.stream()
                .flatMap(t -> {
                    if (t.getFromAccountId() != null && t.getToAccountId() != null) {
                        return java.util.stream.Stream.of(t.getFromAccountId(), t.getToAccountId());
                    } else if (t.getFromAccountId() != null) {
                        return java.util.stream.Stream.of(t.getFromAccountId());
                    } else if (t.getToAccountId() != null) {
                        return java.util.stream.Stream.of(t.getToAccountId());
                    }
                    return java.util.stream.Stream.empty();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(accountRepository.findAllById(accountIds).spliterator(), false)
                .collect(Collectors.toMap(Account::getId, Account::getName));
    }

    private Map<Long, String> loadCategoryData(@NonNull List<Transaction> transactions) {
        Set<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(transactionCategoryRepository.findAllById(categoryIds).spliterator(), false)
                .collect(Collectors.toMap(TransactionCategory::getId, TransactionCategory::getName));
    }

    private Map<Long, String> loadCounterpartyData(@NonNull List<Transaction> transactions) {
        Set<Long> counterpartyIds = transactions.stream()
                .map(Transaction::getCounterpartyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (counterpartyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return StreamSupport.stream(counterpartyRepository.findAllById(counterpartyIds).spliterator(), false)
                .collect(Collectors.toMap(Counterparty::getId, Counterparty::getName));
    }

    private Map<Long, String> loadVehicleData(@NonNull List<Transaction> transactions) {
        List<Long> vehicleIds = transactions.stream()
                .map(Transaction::getVehicleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<Map<Long, String>> vehiclesResponse = vehicleApiClient.getVehicles(vehicleIds).getBody();
            return vehiclesResponse != null
                    ? vehiclesResponse.stream()
                    .flatMap(map -> map.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, _) -> a
                    ))
                    : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch vehicle numbers: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<TransactionPageDTO> mapTransactionsToDTOs(@NonNull List<Transaction> transactions,
                                                           @NonNull Map<Long, String> clientCompanyMap,
                                                           @NonNull Map<Long, String> accountNameMap,
                                                           @NonNull Map<Long, String> categoryNameMap,
                                                           @NonNull Map<Long, String> counterpartyNameMap,
                                                           @NonNull Map<Long, String> vehicleNumberMap) {
        return transactions.stream()
                .map(transaction -> {
                    TransactionPageDTO dto = transactionMapper.transactionToTransactionPageDTO(transaction);
                    dto.setClientCompany(getClientCompany(transaction.getClientId(), clientCompanyMap));
                    dto.setFromAccountName(getAccountName(transaction.getFromAccountId(), accountNameMap));
                    dto.setToAccountName(getAccountName(transaction.getToAccountId(), accountNameMap));
                    dto.setCategoryName(getCategoryName(transaction.getCategoryId(), categoryNameMap));
                    dto.setVehicleNumber(getVehicleNumber(transaction.getVehicleId(), vehicleNumberMap));
                    dto.setCounterpartyName(getCounterpartyName(transaction.getCounterpartyId(), counterpartyNameMap));
                    return dto;
                })
                .toList();
    }

    private String getClientCompany(Long clientId, @NonNull Map<Long, String> clientCompanyMap) {
        return clientId != null ? clientCompanyMap.getOrDefault(clientId, "") : "";
    }

    private String getAccountName(Long accountId, @NonNull Map<Long, String> accountNameMap) {
        return accountId != null ? accountNameMap.getOrDefault(accountId, "") : "";
    }

    private String getCategoryName(Long categoryId, @NonNull Map<Long, String> categoryNameMap) {
        return categoryId != null ? categoryNameMap.getOrDefault(categoryId, "") : "";
    }

    private String getVehicleNumber(Long vehicleId, @NonNull Map<Long, String> vehicleNumberMap) {
        return vehicleId != null ? vehicleNumberMap.getOrDefault(vehicleId, "") : "";
    }

    private String getCounterpartyName(Long counterpartyId, @NonNull Map<Long, String> counterpartyNameMap) {
        return counterpartyId != null ? counterpartyNameMap.getOrDefault(counterpartyId, "") : "";
    }
}
