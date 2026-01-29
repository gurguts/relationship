package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.clients.VehicleApiClient;
import org.example.userservice.models.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransactionSearchDataFetcher {

    public record SearchData(
            Map<Long, String> clientCompanyMap,
            Map<Long, String> accountNameMap,
            Map<Long, String> categoryNameMap,
            Map<Long, String> counterpartyNameMap,
            Map<Long, String> vehicleNumberMap
    ) {
    }

    private final TransactionLookupDataFetcher lookupDataFetcher;
    private final VehicleApiClient vehicleApiClient;

    public SearchData loadSearchData(@NonNull List<Transaction> transactions) {
        return new SearchData(
                lookupDataFetcher.loadClientData(transactions),
                lookupDataFetcher.loadAccountData(transactions),
                lookupDataFetcher.loadCategoryData(transactions),
                lookupDataFetcher.loadCounterpartyData(transactions),
                loadVehicleData(transactions)
        );
    }

    private Map<Long, String> loadVehicleData(List<Transaction> transactions) {
        List<Long> vehicleIds = transactions.stream()
                .map(Transaction::getVehicleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<Map<Long, String>> vehiclesResponse = vehicleApiClient.getVehicles(vehicleIds).getBody();
            return vehiclesResponse != null
                    ? vehiclesResponse.stream()
                    .flatMap(map -> map.entrySet().stream())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, _) -> a
                    ))
                    : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
