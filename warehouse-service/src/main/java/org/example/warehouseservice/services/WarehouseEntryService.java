package org.example.warehouseservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.warehouseservice.clients.PurchaseApiClient;
import org.example.warehouseservice.exceptions.WarehouseException;
import org.example.warehouseservice.models.WarehouseEntry;
import org.example.warehouseservice.models.dto.PageResponse;
import org.example.warehouseservice.models.dto.PurchaseDTO;
import org.example.warehouseservice.models.dto.WarehouseEntryDTO;
import org.example.warehouseservice.repositories.WarehouseEntryRepository;
import org.example.warehouseservice.spec.WarehouseEntrySpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseEntryService {
    private final WarehouseEntryRepository warehouseEntryRepository;
    private final PurchaseApiClient purchaseApiClient;

    public PageResponse<WarehouseEntryDTO> getWarehouseEntries(int page, int size, String sort, String direction, Map<String, List<String>> filters) {
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

        List<PurchaseDTO> purchases;
        try {
            purchases = purchaseApiClient.getPurchasesByFilters(purchaseFilters);
        } catch (Exception e) {
            throw new WarehouseException("PURCHASE","Failed to fetch purchases");
        }

        WarehouseEntrySpecification spec = new WarehouseEntrySpecification(filters);
        Page<WarehouseEntry> warehouseEntryPage = warehouseEntryRepository.findAll(spec, pageRequest);

        Map<String, BigDecimal> purchaseQuantityMap = purchases.stream()
                .collect(Collectors.groupingBy(
                        purchase -> purchase.getUserId() + "-" + purchase.getProductId() + "-" + purchase.getCreatedAt().toLocalDate(),
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                PurchaseDTO::getQuantity,
                                BigDecimal::add
                        )
                ));

        Map<String, WarehouseEntryDTO> dtoMap = new HashMap<>();
        for (PurchaseDTO purchase : purchases) {
            String key = purchase.getUserId() + "-" + purchase.getProductId() + "-" + purchase.getCreatedAt().toLocalDate();
            if (!dtoMap.containsKey(key)) {
                WarehouseEntryDTO dto = new WarehouseEntryDTO();
                dto.setId(null);
                dto.setUserId(purchase.getUserId());
                dto.setProductId(purchase.getProductId());
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
/*        Set<String> warehouseEntryKeys = allWarehouseEntries.getContent().stream()
                .map(entry -> entry.getUserId() + "-" + entry.getProductId() + "-" + entry.getEntryDate())
                .collect(Collectors.toSet());*/

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
        List<WarehouseEntryDTO> pagedContent = start < content.size() ? content.subList(start, end) : Collections.emptyList();

        return new PageResponse<>(page, size, totalElements, totalPages, pagedContent);
    }

    public WarehouseEntryDTO createWarehouseEntry(WarehouseEntryDTO dto) {
        WarehouseEntry entry = new WarehouseEntry();
        entry.setUserId(dto.getUserId());
        entry.setProductId(dto.getProductId());
        entry.setQuantity(dto.getQuantity());
        entry.setEntryDate(dto.getEntryDate());

        WarehouseEntry saved = warehouseEntryRepository.save(entry);

        WarehouseEntryDTO result = new WarehouseEntryDTO();
        result.setId(saved.getId());
        result.setUserId(saved.getUserId());
        result.setProductId(saved.getProductId());
        result.setQuantity(saved.getQuantity());
        result.setEntryDate(saved.getEntryDate());

        return result;
    }
}