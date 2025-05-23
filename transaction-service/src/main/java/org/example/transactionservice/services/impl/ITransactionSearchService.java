package org.example.transactionservice.services.impl;

import org.example.transactionservice.models.PageResponse;
import org.example.transactionservice.models.Transaction;
import org.example.transactionservice.models.dto.TransactionPageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ITransactionSearchService {
    PageResponse<TransactionPageDTO> getTransactionsWithPagination(int page, int size, String sort, String direction, Map<String, List<String>> filters);
}
