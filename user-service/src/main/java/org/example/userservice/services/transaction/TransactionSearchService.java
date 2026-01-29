package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionSearchService implements ITransactionSearchService {
    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionSearchValidator searchValidator;
    private final TransactionSearchDataFetcher dataFetcher;

    @Override
    @NonNull
    @Transactional(readOnly = true)
    public PageResponse<TransactionPageDTO> getTransactionsWithPagination(int page, int size, String sort,
                                                                          String direction, Map<String, List<String>> filters) {
        searchValidator.validateSearchParameters(page, size, sort, direction);

        Sort sortBy = buildSort(sort, direction);
        PageRequest pageRequest = PageRequest.of(page, size, sortBy);

        TransactionSpecification spec = new TransactionSpecification(filters);
        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageRequest);

        TransactionSearchDataFetcher.SearchData data = dataFetcher.loadSearchData(transactionPage.getContent());

        List<TransactionPageDTO> content = mapTransactionsToDTOs(
                transactionPage.getContent(),
                data.clientCompanyMap(),
                data.accountNameMap(),
                data.categoryNameMap(),
                data.counterpartyNameMap(),
                data.vehicleNumberMap()
        );

        return new PageResponse<>(
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                content
        );
    }

    private Sort buildSort(String sort, String direction) {
        String sortField = sort != null && !sort.trim().isEmpty() ? sort : DEFAULT_SORT_FIELD;
        String sortDirection = direction != null && !direction.trim().isEmpty() ? direction : DEFAULT_SORT_DIRECTION;

        try {
            Sort.Direction sortDir = Sort.Direction.fromString(sortDirection);
            return Sort.by(sortDir, sortField);
        } catch (IllegalArgumentException e) {
            return Sort.by(Sort.Direction.fromString(DEFAULT_SORT_DIRECTION), sortField);
        }
    }

    private List<TransactionPageDTO> mapTransactionsToDTOs(List<Transaction> transactions,
                                                          Map<Long, String> clientCompanyMap,
                                                          Map<Long, String> accountNameMap,
                                                          Map<Long, String> categoryNameMap,
                                                          Map<Long, String> counterpartyNameMap,
                                                          Map<Long, String> vehicleNumberMap) {
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

    private String getClientCompany(Long clientId, Map<Long, String> clientCompanyMap) {
        return clientId != null ? clientCompanyMap.getOrDefault(clientId, "") : "";
    }

    private String getAccountName(Long accountId, Map<Long, String> accountNameMap) {
        return accountId != null ? accountNameMap.getOrDefault(accountId, "") : "";
    }

    private String getCategoryName(Long categoryId, Map<Long, String> categoryNameMap) {
        return categoryId != null ? categoryNameMap.getOrDefault(categoryId, "") : "";
    }

    private String getVehicleNumber(Long vehicleId, Map<Long, String> vehicleNumberMap) {
        return vehicleId != null ? vehicleNumberMap.getOrDefault(vehicleId, "") : "";
    }

    private String getCounterpartyName(Long counterpartyId, Map<Long, String> counterpartyNameMap) {
        return counterpartyId != null ? counterpartyNameMap.getOrDefault(counterpartyId, "") : "";
    }
}
