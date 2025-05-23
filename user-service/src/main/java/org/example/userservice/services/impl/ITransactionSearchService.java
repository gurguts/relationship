package org.example.userservice.services.impl;


import org.example.userservice.models.dto.PageResponse;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;

import java.util.List;
import java.util.Map;

public interface ITransactionSearchService {
    PageResponse<TransactionPageDTO> getTransactionsWithPagination(
            int page,
            int size,
            String sort,
            String direction,
            Map<String, List<String>> filters
    );
}
