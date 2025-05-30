package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.exceptions.WarehouseNotFoundException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.warehouse.WarehouseEntry;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.repositories.WarehouseEntryRepository;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.example.purchaseservice.services.impl.IWarehouseEntryService;
import org.example.purchaseservice.spec.WarehouseEntrySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseEntryService implements IWarehouseEntryService {
    private final WarehouseEntryRepository warehouseEntryRepository;
    private final IPurchaseSearchService purchaseSearchService;
    private final WarehouseWithdrawalRepository warehouseWithdrawalRepository;

    @Override
    public PageResponse<WarehouseEntryDTO> getWarehouseEntries(int page, int size, String sort,
                                                               String direction, Map<String, List<String>> filters) {

        PageRequest pageRequest = createPageRequest(page, size, sort, direction);

        LocalDate dateFrom = parseDateFilter(filters, "entry_date_from");
        LocalDate dateTo = parseDateFilter(filters, "entry_date_to");

        Map<String, List<String>> purchaseFilters = buildPurchaseFilters(filters, dateFrom, dateTo);

        List<Purchase> purchases = fetchPurchases(purchaseFilters);

        WarehouseEntrySpecification spec = new WarehouseEntrySpecification(filters);
        Page<WarehouseEntry> warehouseEntryPage = warehouseEntryRepository.findAll(spec, pageRequest);

        Map<String, BigDecimal> purchaseQuantityMap = aggregatePurchaseQuantities(purchases);

        Map<String, WarehouseEntryDTO> dtoMap = buildDtoMap(purchases, purchaseQuantityMap);

        enrichDtoMapWithEntries(warehouseEntryPage.getContent(), purchaseQuantityMap, dtoMap);

        enrichDtoMapWithAllEntries(spec, dtoMap);

        List<WarehouseEntryDTO> content = sortAndPaginate(dtoMap.values(), page, size, direction);

        return buildPageResponse(content, page, size);
    }

    private PageRequest createPageRequest(int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        return PageRequest.of(page, size, sortBy);
    }

    private LocalDate parseDateFilter(Map<String, List<String>> filters, String key) {
        return filters.containsKey(key) && !filters.get(key).isEmpty()
                ? LocalDate.parse(filters.get(key).getFirst(), DateTimeFormatter.ISO_LOCAL_DATE)
                : null;
    }

    private Map<String, List<String>> buildPurchaseFilters(
            Map<String, List<String>> filters, LocalDate dateFrom, LocalDate dateTo) {
        Map<String, List<String>> purchaseFilters = new HashMap<>();
        if (dateFrom != null) {
            purchaseFilters.put("createdAtFrom", List.of(dateFrom.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
        if (dateTo != null) {
            purchaseFilters.put("createdAtTo", List.of(dateTo.format(DateTimeFormatter.ISO_LOCAL_DATE)));
        }
        if (filters.containsKey("user_id") && !filters.get("user_id").isEmpty()) {
            purchaseFilters.put("user", filters.get("user_id"));
        }
        if (filters.containsKey("product_id") && !filters.get("product_id").isEmpty()) {
            purchaseFilters.put("product", filters.get("product_id"));
        }
        return purchaseFilters;
    }

    private List<Purchase> fetchPurchases(Map<String, List<String>> purchaseFilters) {
        try {
            return purchaseSearchService.searchForWarehouse(purchaseFilters);
        } catch (Exception e) {
            throw new WarehouseException("PURCHASE", "Failed to fetch purchases");
        }
    }

    private Map<String, BigDecimal> aggregatePurchaseQuantities(List<Purchase> purchases) {
        return purchases.stream()
                .collect(Collectors.groupingBy(
                        purchase -> purchase.getUser() + "-" + purchase.getProduct() + "-"
                                + purchase.getCreatedAt().toLocalDate(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Purchase::getQuantity,
                                BigDecimal::add
                        )
                ));
    }

    private Map<String, WarehouseEntryDTO> buildDtoMap(
            List<Purchase> purchases, Map<String, BigDecimal> purchaseQuantityMap) {
        Map<String, WarehouseEntryDTO> dtoMap = new HashMap<>();
        for (Purchase purchase : purchases) {
            String key = purchase.getUser() + "-" + purchase.getProduct() + "-" + purchase.getCreatedAt().toLocalDate();
            if (!dtoMap.containsKey(key)) {
                WarehouseEntryDTO dto = new WarehouseEntryDTO();
                dto.setId(null);
                dto.setUserId(purchase.getUser());
                dto.setProductId(purchase.getProduct());
                dto.setQuantity(BigDecimal.ZERO);
                dto.setEntryDate(purchase.getCreatedAt().toLocalDate());
                dto.setPurchasedQuantity(purchaseQuantityMap.getOrDefault(key, BigDecimal.ZERO));
                dtoMap.put(key, dto);
            }
        }
        return dtoMap;
    }

    private void enrichDtoMapWithEntries(List<WarehouseEntry> entries, Map<String, BigDecimal> purchaseQuantityMap,
                                         Map<String, WarehouseEntryDTO> dtoMap) {
        for (WarehouseEntry entry : entries) {
            String key = entry.getUserId() + "-" + entry.getProductId() + "-" + entry.getEntryDate();
            WarehouseEntryDTO dto = dtoMap.computeIfAbsent(key, _ -> new WarehouseEntryDTO());
            dto.setId(entry.getId());
            dto.setUserId(entry.getUserId());
            dto.setProductId(entry.getProductId());
            dto.setWarehouseId(entry.getWarehouseId());
            dto.setQuantity(entry.getQuantity());
            dto.setEntryDate(entry.getEntryDate());
            dto.setPurchasedQuantity(purchaseQuantityMap.getOrDefault(key, BigDecimal.ZERO));
        }
    }

    private void enrichDtoMapWithAllEntries(WarehouseEntrySpecification spec,
                                            Map<String, WarehouseEntryDTO> dtoMap) {
        PageRequest allEntriesRequest = PageRequest.of(0, Integer.MAX_VALUE);
        Page<WarehouseEntry> allWarehouseEntries = warehouseEntryRepository.findAll(spec, allEntriesRequest);
        for (WarehouseEntry entry : allWarehouseEntries.getContent()) {
            String key = entry.getUserId() + "-" + entry.getProductId() + "-" + entry.getEntryDate();
            if (!dtoMap.containsKey(key)) {
                WarehouseEntryDTO dto = new WarehouseEntryDTO();
                dto.setId(entry.getId());
                dto.setUserId(entry.getUserId());
                dto.setProductId(entry.getProductId());
                dto.setWarehouseId(entry.getWarehouseId());
                dto.setQuantity(entry.getQuantity());
                dto.setEntryDate(entry.getEntryDate());
                dto.setPurchasedQuantity(BigDecimal.ZERO);
                dtoMap.put(key, dto);
            }
        }
    }

    private List<WarehouseEntryDTO> sortAndPaginate(Collection<WarehouseEntryDTO> dtos, int page, int size,
                                                    String direction) {
        List<WarehouseEntryDTO> content = new ArrayList<>(dtos);
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        content.sort((dto1, dto2) -> {
            int dateCompare = sortDirection.isAscending() ?
                    dto1.getEntryDate().compareTo(dto2.getEntryDate()) :
                    dto2.getEntryDate().compareTo(dto1.getEntryDate());
            if (dateCompare != 0) return dateCompare;
            int userCompare = dto1.getUserId().compareTo(dto2.getUserId());
            if (userCompare != 0) return userCompare;
            return dto1.getProductId().compareTo(dto2.getProductId());
        });

        int start = page * size;
        int end = Math.min(start + size, content.size());
        return start < content.size() ? content.subList(start, end) : Collections.emptyList();
    }

    private PageResponse<WarehouseEntryDTO> buildPageResponse(List<WarehouseEntryDTO> content, int page, int size) {
        int totalElements = content.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(page, size, totalElements, totalPages, content);
    }

    @Override
    @Transactional
    public WarehouseEntry createWarehouseEntry(WarehouseEntry warehouseEntry) {
        Optional<WarehouseEntry> existingEntry = warehouseEntryRepository
                .findByUserIdAndProductIdAndEntryDate(
                        warehouseEntry.getUserId(),
                        warehouseEntry.getProductId(),
                        warehouseEntry.getEntryDate()
                );

        if (existingEntry.isPresent()) {
            throw new WarehouseException("ALREADY_CREATED",
                    "Warehouse entry with the same userId, productId, and entryDate already exists");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long executorUserId = (Long) authentication.getDetails();
        warehouseEntry.setExecutorUserId(executorUserId);

        return warehouseEntryRepository.save(warehouseEntry);
    }

    @Override
    @Transactional
    public void updateWarehouseEntry(Long warehouseEntryId, BigDecimal newQuantity) {
        if (warehouseEntryId == null) {
            throw new WarehouseException("Warehouse ID cannot be null");
        }
        if (newQuantity == null) {
            throw new WarehouseException("New quantity cannot be null");
        }

        WarehouseEntry entry = warehouseEntryRepository.findById(warehouseEntryId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        String.format("Warehouse entry with ID %d not found", warehouseEntryId)));

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            warehouseEntryRepository.delete(entry);
        } else {
            entry.setQuantity(newQuantity);
            warehouseEntryRepository.save(entry);
        }
    }


    @Override
    public List<WarehouseEntry> findWarehouseEntriesByFilters(Map<String, List<String>> filters) {
        Specification<WarehouseEntry> spec = new WarehouseEntrySpecification(filters);
        return warehouseEntryRepository.findAll(spec);
    }


    @Override
    public Map<Long, Map<Long, Double>> getWarehouseBalance() {
        try {

            Map<Long, Map<Long, BigDecimal>> totalWarehouseEntry = calculateTotalEntries();
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals = calculateTotalWithdrawals();
            return Collections.unmodifiableMap(calculateBalance(totalWarehouseEntry, totalWithdrawals));

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new WarehouseException("UNABLE_CALCULATE_WAREHOUSE_BALANCES", e.getMessage());
        }
    }

    private <T> Map<Long, Map<Long, BigDecimal>> calculateTotals(
            List<T> records,
            Function<T, Long> warehouseIdExtractor,
            Function<T, Long> productIdExtractor,
            Function<T, BigDecimal> quantityExtractor) {
        return records.stream()
                .filter(record -> isValidRecord(record, warehouseIdExtractor, productIdExtractor, quantityExtractor))
                .collect(Collectors.groupingBy(
                        warehouseIdExtractor,
                        Collectors.groupingBy(
                                productIdExtractor,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        quantityExtractor,
                                        BigDecimal::add
                                )
                        )
                ));
    }

    private <T> boolean isValidRecord(T record, Function<T, Long> warehouseIdExtractor,
                                      Function<T, Long> productIdExtractor, Function<T, BigDecimal> quantityExtractor) {
        if (warehouseIdExtractor.apply(record) == null ||
                productIdExtractor.apply(record) == null ||
                quantityExtractor.apply(record) == null) {
            log.warn("Invalid entry detected: {}", record);
            return false;
        }
        return true;
    }

    private Map<Long, Map<Long, BigDecimal>> calculateTotalEntries() {
        return calculateTotals(
                warehouseEntryRepository.findAll(),
                WarehouseEntry::getWarehouseId,
                WarehouseEntry::getProductId,
                WarehouseEntry::getQuantity
        );
    }

    private Map<Long, Map<Long, BigDecimal>> calculateTotalWithdrawals() {
        return calculateTotals(
                warehouseWithdrawalRepository.findAll(),
                WarehouseWithdrawal::getWarehouseId,
                WarehouseWithdrawal::getProductId,
                WarehouseWithdrawal::getQuantity
        );
    }

    private Map<Long, Map<Long, Double>> calculateBalance(
            Map<Long, Map<Long, BigDecimal>> totalWarehouseEntry,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Map<Long, Map<Long, Double>> balanceByWarehouseAndProduct = new HashMap<>();
        Set<Long> allWarehouseIds = getAllWarehouseIds(totalWarehouseEntry, totalWithdrawals);

        for (Long warehouseId : allWarehouseIds) {
            balanceByWarehouseAndProduct.computeIfAbsent(warehouseId, _ -> new HashMap<>())
                    .putAll(calculateBalanceForWarehouse(warehouseId, totalWarehouseEntry, totalWithdrawals));
        }

        return balanceByWarehouseAndProduct;
    }

    private Set<Long> getAllWarehouseIds(
            Map<Long, Map<Long, BigDecimal>> totalWarehouseEntry,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Set<Long> allWarehouseIds = new HashSet<>();
        allWarehouseIds.addAll(totalWarehouseEntry.keySet());
        allWarehouseIds.addAll(totalWithdrawals.keySet());
        return allWarehouseIds;
    }

    private Map<Long, Double> calculateBalanceForWarehouse(
            Long warehouseId,
            Map<Long, Map<Long, BigDecimal>> totalWarehouseEntry,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Map<Long, Double> balanceByProduct = new HashMap<>();
        Set<Long> allProductIds = getAllProductIds(warehouseId, totalWarehouseEntry, totalWithdrawals);

        for (Long productId : allProductIds) {
            BigDecimal entries = totalWarehouseEntry.getOrDefault(warehouseId, Collections.emptyMap())
                    .getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal withdrawals = totalWithdrawals.getOrDefault(warehouseId, Collections.emptyMap())
                    .getOrDefault(productId, BigDecimal.ZERO);
            balanceByProduct.put(productId, entries.subtract(withdrawals).doubleValue());
        }

        return balanceByProduct;
    }

    private Set<Long> getAllProductIds(
            Long warehouseId,
            Map<Long, Map<Long, BigDecimal>> totalWarehouseEntry,
            Map<Long, Map<Long, BigDecimal>> totalWithdrawals) {
        Set<Long> allProductIds = new HashSet<>();
        if (totalWarehouseEntry.containsKey(warehouseId)) {
            allProductIds.addAll(totalWarehouseEntry.get(warehouseId).keySet());
        }
        if (totalWithdrawals.containsKey(warehouseId)) {
            allProductIds.addAll(totalWithdrawals.get(warehouseId).keySet());
        }
        return allProductIds;
    }
}