package org.example.userservice.restControllers.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.models.dto.PageResponse;
import org.example.userservice.models.dto.transaction.TransactionPageDTO;
import org.example.userservice.services.impl.ITransactionSearchService;
import org.example.userservice.services.transaction.TransactionExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/transaction")
@RequiredArgsConstructor
@Validated
public class TransactionSearchController {
    private static final String FILENAME_PREFIX = "transactions_";
    private static final String FILENAME_SUFFIX = ".xlsx";
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd_HH-mm-ss";

    private final ITransactionSearchService transactionSearchService;
    private final TransactionExportService transactionExportService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/search")
    public ResponseEntity<PageResponse<TransactionPageDTO>> searchTransactions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) {

        Map<String, List<String>> filterMap = parseFilters(filters);

        PageResponse<TransactionPageDTO> result =
                transactionSearchService.getTransactionsWithPagination(page, size, sort, direction, filterMap);

        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAuthority('finance:view')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String filters) throws IOException {

        Map<String, List<String>> filterMap = parseFilters(filters);

        byte[] excelData = transactionExportService.exportToExcel(filterMap);

        String filename = FILENAME_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)) + FILENAME_SUFFIX;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    private Map<String, List<String>> parseFilters(String filters) {
        try {
            if (filters != null && !filters.isEmpty()) {
                return objectMapper.readValue(filters, objectMapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, List.class));
            } else {
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Failed to parse filters: {}", filters, e);
            return Collections.emptyMap();
        }
    }
}
