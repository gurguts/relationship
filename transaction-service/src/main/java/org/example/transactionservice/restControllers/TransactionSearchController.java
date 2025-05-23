package org.example.transactionservice.restControllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.transactionservice.models.PageResponse;
import org.example.transactionservice.models.dto.TransactionPageDTO;
import org.example.transactionservice.services.impl.ITransactionSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Slf4j
public class TransactionSearchController {
    private final ITransactionSearchService transactionSearchService;

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<TransactionPageDTO>> searchTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {

        Map<String, List<String>> filterMap = filters != null && !filters.isEmpty()
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {})
                : Collections.emptyMap();

        PageResponse<TransactionPageDTO> result = transactionSearchService.getTransactionsWithPagination(page, size, sort, direction, filterMap);

        return ResponseEntity.ok(result);
    }

}
