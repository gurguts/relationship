package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.clients.ClientApiClient;
import org.example.userservice.models.account.Account;
import org.example.userservice.models.transaction.Counterparty;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.models.transaction.TransactionCategory;
import org.example.userservice.repositories.AccountRepository;
import org.example.userservice.repositories.CounterpartyRepository;
import org.example.userservice.repositories.TransactionCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class TransactionLookupDataFetcher {

    private final ClientApiClient clientApiClient;
    private final AccountRepository accountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final CounterpartyRepository counterpartyRepository;

    public Map<Long, String> loadClientData(@NonNull List<Transaction> transactions) {
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
            return Collections.emptyMap();
        }
    }

    public Map<Long, String> loadAccountData(@NonNull List<Transaction> transactions) {
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

        return fetchMapByIds(accountIds, accountRepository.findAllById(accountIds), Account::getId, Account::getName);
    }

    public Map<Long, String> loadCategoryData(@NonNull List<Transaction> transactions) {
        Set<Long> categoryIds = transactions.stream()
                .map(Transaction::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return fetchMapByIds(categoryIds, transactionCategoryRepository.findAllById(categoryIds), TransactionCategory::getId, TransactionCategory::getName);
    }

    public Map<Long, String> loadCounterpartyData(@NonNull List<Transaction> transactions) {
        Set<Long> counterpartyIds = transactions.stream()
                .map(Transaction::getCounterpartyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return fetchMapByIds(counterpartyIds, counterpartyRepository.findAllById(counterpartyIds), Counterparty::getId, Counterparty::getName);
    }

    private <T> Map<Long, String> fetchMapByIds(Set<Long> ids, Iterable<T> entities, Function<T, Long> idGetter, Function<T, String> nameGetter) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return StreamSupport.stream(entities.spliterator(), false)
                .collect(Collectors.toMap(idGetter, nameGetter));
    }
}
