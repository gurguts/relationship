package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Product;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.repositories.ProductRepository;
import org.example.purchaseservice.services.impl.IVehicleExpenseService;
import org.example.purchaseservice.services.impl.IVehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExportDataFetcher {
    
    private final IVehicleExpenseService vehicleExpenseService;
    private final IVehicleService vehicleService;
    private final ProductRepository productRepository;
    private final org.example.purchaseservice.clients.TransactionCategoryClient transactionCategoryClient;
    private final org.example.purchaseservice.clients.AccountClient accountClient;

    public record VehicleData(
            List<Vehicle> vehicles,
            Map<Long, Product> productMap,
            Map<Long, List<VehicleExpense>> expensesMap,
            Map<Long, String> accountNameMap,
            Map<Long, String> categoryNameMap,
            List<Long> sortedCategoryIds
    ) {}
    
    public VehicleData loadVehicleData(@NonNull List<Vehicle> vehicles) {
        List<Long> vehicleIds = vehicles.stream()
                .map(Vehicle::getId)
                .toList();
        
        Map<Long, Product> productMap = loadProductMap(vehicleIds);
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap = 
                vehicleExpenseService.getExpensesByVehicleIds(vehicleIds);
        
        ExpenseIds expenseIds = extractExpenseIds(expensesMap);
        Set<Long> categoryIds = expenseIds.categoryIds();
        Set<Long> accountIds = expenseIds.accountIds();
        
        Map<Long, String> accountNameMap = loadAccountNames(accountIds);
        Map<Long, String> categoryNameMap = loadCategoryNames(categoryIds);
        List<Long> sortedCategoryIds = sortCategoryIds(categoryIds, categoryNameMap);
        
        return new VehicleData(vehicles, productMap, expensesMap, accountNameMap, categoryNameMap, sortedCategoryIds);
    }
    
    private Map<Long, Product> loadProductMap(List<Long> vehicleIds) {
        Map<Long, List<org.example.purchaseservice.models.balance.VehicleProduct>> vehicleProductsMap = 
                vehicleService.getVehicleProductsByVehicleIds(vehicleIds);
        List<org.example.purchaseservice.models.balance.VehicleProduct> allVehicleProducts = 
                vehicleProductsMap.values().stream()
                        .flatMap(List::stream)
                        .toList();
        
        List<Long> productIds = allVehicleProducts.stream()
                .map(org.example.purchaseservice.models.balance.VehicleProduct::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Iterable<Product> productsIterable = productRepository.findAllById(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : productsIterable) {
            if (product != null && product.getId() != null) {
                productMap.put(product.getId(), product);
            }
        }
        return productMap;
    }
    
    private ExpenseIds extractExpenseIds(Map<Long, List<org.example.purchaseservice.models.balance.VehicleExpense>> expensesMap) {
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> accountIds = new HashSet<>();
        
        expensesMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .forEach(expense -> {
                    Long categoryId = expense.getCategoryId();
                    if (categoryId != null) {
                        categoryIds.add(categoryId);
                    }
                    Long accountId = expense.getFromAccountId();
                    if (accountId != null) {
                        accountIds.add(accountId);
                    }
                });
        
        return new ExpenseIds(categoryIds, accountIds);
    }
    
    private Map<Long, String> loadAccountNames(Set<Long> accountIds) {
        if (isEmpty(accountIds)) {
            return Collections.emptyMap();
        }
        
        Map<Long, String> accountNameMap = new HashMap<>();
        try {
            List<org.example.purchaseservice.models.dto.account.AccountDTO> accounts = accountClient.getAllAccounts().getBody();
            if (accounts != null) {
                accounts.stream()
                        .filter(account -> account != null && account.getId() != null && accountIds.contains(account.getId()))
                        .forEach(account -> accountNameMap.put(account.getId(), account.getName()));
            }
        } catch (Exception _) {
        }
        return accountNameMap;
    }

    private Map<Long, String> loadCategoryNames(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ResponseEntity<Map<Long, String>> response =
                    transactionCategoryClient.getCategoryNamesByIds(categoryIds);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Failed to load names of categories, statuses: {}", response.getStatusCode());
                return emptyNamesMap(categoryIds);
            }
        } catch (Exception e) {
            log.error("Error while batch loading category names", e);
            return emptyNamesMap(categoryIds);
        }
    }

    private Map<Long, String> emptyNamesMap(Set<Long> categoryIds) {
        Map<Long, String> result = new HashMap<>();
        for (Long id : categoryIds) {
            result.put(id, "");
        }
        return result;
    }
    
    private List<Long> sortCategoryIds(Set<Long> categoryIds, Map<Long, String> categoryNameMap) {
        return categoryIds.stream()
                .sorted((id1, id2) -> {
                    String name1 = categoryNameMap.getOrDefault(id1, "");
                    String name2 = categoryNameMap.getOrDefault(id2, "");
                    return name1.compareToIgnoreCase(name2);
                })
                .toList();
    }
    
    private boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    private record ExpenseIds(Set<Long> categoryIds, Set<Long> accountIds) {}
}
