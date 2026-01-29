package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.transaction.Transaction;
import org.example.userservice.repositories.TransactionRepository;
import org.example.userservice.services.impl.ITransactionExportService;
import org.example.userservice.spec.TransactionSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionExportService implements ITransactionExportService {
    private static final String ERROR_CODE_EXPORT_FAILED = "EXPORT_FAILED";

    private final TransactionRepository transactionRepository;
    private final TransactionExportDataFetcher dataFetcher;
    private final TransactionExcelBuilder excelBuilder;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(@NonNull Map<String, List<String>> filters) {
        try {
            TransactionSpecification spec = new TransactionSpecification(filters);
            Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
            List<Transaction> transactions = transactionRepository.findAll(spec, sortBy);

            if (transactions.isEmpty()) {
                return excelBuilder.buildEmpty();
            }

            TransactionExportDataFetcher.ExportData data = dataFetcher.loadExportData(transactions);
            return excelBuilder.buildWithData(
                    transactions,
                    data.clientCompanyMap(),
                    data.accountNameMap(),
                    data.categoryNameMap(),
                    data.counterpartyNameMap()
            );
        } catch (Exception e) {
            log.error("Failed to export transactions: {}", e.getMessage(), e);
            throw new TransactionException(ERROR_CODE_EXPORT_FAILED, "Failed to export transactions: " + e.getMessage());
        }
    }
}
