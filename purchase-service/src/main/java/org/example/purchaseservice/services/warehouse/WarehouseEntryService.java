package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WarehouseException;
import org.example.purchaseservice.exceptions.WarehouseNotFoundException;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.WarehouseEntry;
import org.example.purchaseservice.models.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.*;
import org.example.purchaseservice.repositories.WarehouseEntryRepository;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
import org.example.purchaseservice.services.impl.IPurchaseSearchService;
import org.example.purchaseservice.services.impl.IWarehouseEntryService;
import org.example.purchaseservice.spec.WarehouseEntrySpecification;
import org.jetbrains.annotations.NotNull;
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
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sortBy = Sort.by(sortDirection, sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortBy);

        LocalDate dateFrom = filters.containsKey("entry_date_from") && !filters.get("entry_date_from").isEmpty()
                ? LocalDate.parse(filters.get("entry_date_from").getFirst(), DateTimeFormatter.ISO_LOCAL_DATE)
                : null;
        LocalDate dateTo = filters.containsKey("entry_date_to") && !filters.get("entry_date_to").isEmpty()
                ? LocalDate.parse(filters.get("entry_date_to").getFirst(), DateTimeFormatter.ISO_LOCAL_DATE)
                : null;

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

        List<Purchase> purchases;
        try {
            purchases = purchaseSearchService.searchForWarehouse(purchaseFilters);

        } catch (Exception e) {
            throw new WarehouseException("PURCHASE", "Failed to fetch purchases");
        }

        WarehouseEntrySpecification spec = new WarehouseEntrySpecification(filters);
        Page<WarehouseEntry> warehouseEntryPage = warehouseEntryRepository.findAll(spec, pageRequest);

        Map<String, BigDecimal> purchaseQuantityMap = purchases.stream()
                .collect(Collectors.groupingBy(
                        purchase -> purchase.getUser() + "-" + purchase.getProduct() + "-"
                                + purchase.getCreatedAt().toLocalDate(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Purchase::getQuantity,
                                BigDecimal::add
                        )
                ));

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

        for (WarehouseEntry entry : warehouseEntryPage.getContent()) {
            String key = entry.getUserId() + "-" + entry.getProductId() + "-" + entry.getEntryDate();
            WarehouseEntryDTO dto = dtoMap.computeIfAbsent(key, _ -> new WarehouseEntryDTO());
            dto.setId(entry.getId());
            dto.setUserId(entry.getUserId());
            dto.setProductId(entry.getProductId());
            dto.setQuantity(entry.getQuantity());
            dto.setEntryDate(entry.getEntryDate());
            dto.setPurchasedQuantity(purchaseQuantityMap.getOrDefault(key, BigDecimal.ZERO));
        }

        PageRequest allEntriesRequest = PageRequest.of(0, Integer.MAX_VALUE);
        Page<WarehouseEntry> allWarehouseEntries = warehouseEntryRepository.findAll(spec, allEntriesRequest);
        for (WarehouseEntry entry : allWarehouseEntries.getContent()) {
            String key = entry.getUserId() + "-" + entry.getProductId() + "-" + entry.getEntryDate();
            if (!dtoMap.containsKey(key)) {
                WarehouseEntryDTO dto = new WarehouseEntryDTO();
                dto.setId(entry.getId());
                dto.setUserId(entry.getUserId());
                dto.setProductId(entry.getProductId());
                dto.setQuantity(entry.getQuantity());
                dto.setEntryDate(entry.getEntryDate());
                dto.setPurchasedQuantity(BigDecimal.ZERO);
                dtoMap.put(key, dto);
            }
        }

        List<WarehouseEntryDTO> content = new ArrayList<>(dtoMap.values());
        content.sort((dto1, dto2) -> {
            int dateCompare = sortDirection.isAscending() ?
                    dto1.getEntryDate().compareTo(dto2.getEntryDate()) :
                    dto2.getEntryDate().compareTo(dto1.getEntryDate());
            if (dateCompare != 0) return dateCompare;
            int userCompare = dto1.getUserId().compareTo(dto2.getUserId());
            if (userCompare != 0) return userCompare;
            return dto1.getProductId().compareTo(dto2.getProductId());
        });

        int totalElements = dtoMap.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, content.size());
        List<WarehouseEntryDTO> pagedContent =
                start < content.size() ? content.subList(start, end) : Collections.emptyList();

        return new PageResponse<>(page, size, totalElements, totalPages, pagedContent);
    }

    @Override
    @Transactional
    public WarehouseEntryDTO createWarehouseEntry(WarehouseEntryDTO dto) {
        Optional<WarehouseEntry> existingEntry = warehouseEntryRepository
                .findByUserIdAndProductIdAndEntryDate(
                        dto.getUserId(),
                        dto.getProductId(),
                        dto.getEntryDate()
                );

        WarehouseEntry entry = getWarehouseEntry(dto, existingEntry);

        WarehouseEntry saved = warehouseEntryRepository.save(entry);

        WarehouseEntryDTO result = new WarehouseEntryDTO();
        result.setId(saved.getId());
        result.setUserId(saved.getUserId());
        result.setProductId(saved.getProductId());
        result.setQuantity(saved.getQuantity());
        result.setEntryDate(saved.getEntryDate());

        return result;
    }

    private static @NotNull WarehouseEntry getWarehouseEntry(WarehouseEntryDTO dto,
                                                             Optional<WarehouseEntry> existingEntry) {
        if (existingEntry.isPresent()) {
            throw new WarehouseException("ALREADY_CREATED",
                    "Warehouse entry with the same userId, productId, and entryDate already exists");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long executorUserId = (Long) authentication.getDetails();

        // Create new entry since no matching entry was found
        WarehouseEntry entry = new WarehouseEntry();
        entry.setUserId(dto.getUserId());
        entry.setProductId(dto.getProductId());
        entry.setQuantity(dto.getQuantity());
        entry.setEntryDate(dto.getEntryDate());
        entry.setExecutorUserId(executorUserId);
        return entry;
    }

    @Override
    @Transactional
    public void updateWarehouseEntry(Long warehouseId, BigDecimal newQuantity) {
        if (warehouseId == null) {
            throw new IllegalArgumentException("Warehouse ID cannot be null");
        }
        if (newQuantity == null) {
            throw new IllegalArgumentException("New quantity cannot be null");
        }

        WarehouseEntry entry = warehouseEntryRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException(
                        String.format("Warehouse entry with ID %d not found", warehouseId)));

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
    public BalanceWarehouseDTO getWarehouseBalance() {
        Map<Long, BigDecimal> totalWarehouseEntry = warehouseEntryRepository.findAll().stream()
                .filter(p -> p.getProductId() != null && p.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        WarehouseEntry::getProductId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                WarehouseEntry::getQuantity,
                                BigDecimal::add
                        )
                ));

        Map<Long, BigDecimal> totalWithdrawals = warehouseWithdrawalRepository.findAll().stream()
                .filter(w -> w.getProductId() != null && w.getQuantity() != null)
                .collect(Collectors.groupingBy(
                        WarehouseWithdrawal::getProductId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                WarehouseWithdrawal::getQuantity,
                                BigDecimal::add
                        )
                ));

        Map<Long, Double> balanceByProduct = new HashMap<>();
        for (Long productId : totalWarehouseEntry.keySet()) {
            BigDecimal warehouseEntries = totalWarehouseEntry.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal withdrawals = totalWithdrawals.getOrDefault(productId, BigDecimal.ZERO);
            balanceByProduct.put(productId, warehouseEntries.subtract(withdrawals).doubleValue());
        }

        return BalanceWarehouseDTO.builder()
                .balanceByProduct(balanceByProduct)
                .build();
    }

}