package org.example.userservice.services.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.clients.ClientApiClient;
import org.example.userservice.mappers.TransactionMapper;
import org.example.userservice.models.dto.PageResponse;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionSearchService;
import org.example.userservice.spec.TransactionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSearchService implements ITransactionSearchService {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ClientApiClient clientApiClient;

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

        List<TransactionPageDTO> content = transactionPage.getContent().stream()
                .map(transaction -> {
                    TransactionPageDTO dto = transactionMapper.transactionToTransactionPageDTO(transaction);
                    dto.setClientCompany(transaction.getClientId() != null ? clientCompanyMap.getOrDefault(
                            transaction.getClientId(), "") : "");
                    return dto;
                })
                .toList();

        return new PageResponse<>(transactionPage.getNumber(), transactionPage.getSize(),
                transactionPage.getTotalElements(), transactionPage.getTotalPages(), content);
    }
}
