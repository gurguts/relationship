package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TransactionExportDataFetcher {

    public record ExportData(
            Map<Long, String> clientCompanyMap,
            Map<Long, String> accountNameMap,
            Map<Long, String> categoryNameMap,
            Map<Long, String> counterpartyNameMap
    ) {
    }

    private final TransactionLookupDataFetcher lookupDataFetcher;

    public ExportData loadExportData(@NonNull List<Transaction> transactions) {
        return new ExportData(
                lookupDataFetcher.loadClientData(transactions),
                lookupDataFetcher.loadAccountData(transactions),
                lookupDataFetcher.loadCategoryData(transactions),
                lookupDataFetcher.loadCounterpartyData(transactions)
        );
    }
}
