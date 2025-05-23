package org.example.userservice.restControllers.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.dto.PageResponse;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.services.impl.ITransactionSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {
        })
                : Collections.emptyMap();

        PageResponse<TransactionPageDTO> result =
                transactionSearchService.getTransactionsWithPagination(page, size, sort, direction, filterMap);

        return ResponseEntity.ok(result);
    }

}
