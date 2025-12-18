package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.clients.ClientApiClient;
import org.example.userservice.clients.VehicleApiClient;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.dto.PageResponse;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.repositories.AccountRepository;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSearchService implements ITransactionSearchService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ClientApiClient clientApiClient;
    private final VehicleApiClient vehicleApiClient;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<TransactionPageDTO> getTransactionsWithPagination(int page, int size, String sort,
                                                                          String direction, Map<String,
            List<String>> filters) {

        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortBy);

        TransactionSpecification spec = new TransactionSpecification(filters);
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageRequest);

        List<Long> clientIds = transactionPage.getContent().stream()
                .map(Transaction::getClientId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> clientCompanyMap = clientIds.isEmpty()
                ? Collections.emptyMap()
                : clientApiClient.getClients(clientIds).stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, _) -> a
                ));

        // Get account IDs
        Set<Long> accountIds = transactionPage.getContent().stream()
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

        Map<Long, String> accountNameMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : StreamSupport.stream(accountRepository.findAllById(accountIds).spliterator(), false)
                .collect(Collectors.toMap(Account::getId, Account::getName));

        // Get category IDs
        Set<Long> categoryIds = transactionPage.getContent().stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = categoryIds.isEmpty()
                ? Collections.emptyMap()
                : StreamSupport.stream(transactionCategoryRepository.findAllById(categoryIds).spliterator(), false)
                .collect(Collectors.toMap(TransactionCategory::getId, TransactionCategory::getName));

        List<Long> vehicleIds = transactionPage.getContent().stream()
                .map(Transaction::getVehicleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> vehicleNumberMap;
        if (vehicleIds.isEmpty()) {
            vehicleNumberMap = Collections.emptyMap();
        } else {
            try {
                List<Map<Long, String>> vehiclesResponse = vehicleApiClient.getVehicles(vehicleIds);
                vehicleNumberMap = vehiclesResponse.stream()
                        .flatMap(map -> map.entrySet().stream())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, _) -> a
                        ));
            } catch (Exception e) {
                log.warn("Failed to fetch vehicle numbers: {}", e.getMessage());
                vehicleNumberMap = Collections.emptyMap();
            }
        }
        final Map<Long, String> finalVehicleNumberMap = vehicleNumberMap;

        List<TransactionPageDTO> content = transactionPage.getContent().stream()
                .map(transaction -> {
                    TransactionPageDTO dto = transactionMapper.transactionToTransactionPageDTO(transaction);
                    dto.setClientCompany(transaction.getClientId() != null ? clientCompanyMap.getOrDefault(
                            transaction.getClientId(), "") : "");
                    dto.setFromAccountName(transaction.getFromAccountId() != null 
                            ? accountNameMap.getOrDefault(transaction.getFromAccountId(), "") : "");
                    dto.setToAccountName(transaction.getToAccountId() != null 
                            ? accountNameMap.getOrDefault(transaction.getToAccountId(), "") : "");
                    dto.setCategoryName(transaction.getCategoryId() != null 
                            ? categoryNameMap.getOrDefault(transaction.getCategoryId(), "") : "");
                    dto.setVehicleNumber(transaction.getVehicleId() != null 
                            ? finalVehicleNumberMap.getOrDefault(transaction.getVehicleId(), "") : "");
                    return dto;
                })
                .toList();

        return new PageResponse<>(transactionPage.getNumber(), transactionPage.getSize(),
                transactionPage.getTotalElements(), transactionPage.getTotalPages(), content);
    }
}
