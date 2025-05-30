package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.repositories.WarehouseWithdrawalRepository;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
import org.example.purchaseservice.spec.WarehouseWithdrawalSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseWithdrawService implements IWarehouseWithdrawService {
    private final WarehouseWithdrawalRepository warehouseWithdrawalRepository;


    @Override
    @Transactional
    public WarehouseWithdrawal createWithdrawal(WarehouseWithdrawal warehouseWithdrawal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();

        warehouseWithdrawal.setUserId(userId);

        return warehouseWithdrawalRepository.save(warehouseWithdrawal);
    }

    @Override
    @Transactional
    public WarehouseWithdrawal updateWithdrawal(Long id, WarehouseWithdrawal request) {
        WarehouseWithdrawal withdrawal = warehouseWithdrawalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found"));

        withdrawal.setReasonType(request.getReasonType());
        withdrawal.setQuantity(request.getQuantity());
        withdrawal.setDescription(request.getDescription());
        withdrawal.setWithdrawalDate(request.getWithdrawalDate());

        return warehouseWithdrawalRepository.save(withdrawal);
    }

    @Override
    @Transactional
    public void deleteWithdrawal(Long id) {
        warehouseWithdrawalRepository.deleteById(id);
    }

    @Override
    public PageResponse<WithdrawalDTO> getWithdrawals(int page, int size, String sort, String direction,
                                                      Map<String, List<String>> filters) {
        Sort sortOrder = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageRequest = PageRequest.of(page, size, sortOrder);
        WarehouseWithdrawalSpecification spec = new WarehouseWithdrawalSpecification(filters);
        Page<WarehouseWithdrawal> withdrawalPage = warehouseWithdrawalRepository.findAll(spec, pageRequest);

        List<WithdrawalDTO> content = withdrawalPage.getContent().stream()
                .map(w -> WithdrawalDTO.builder()
                        .id(w.getId())
                        .productId(w.getProductId())
                        .warehouseId(w.getWarehouseId())
                        .userId(w.getUserId())
                        .reasonType(w.getReasonType().name())
                        .quantity(w.getQuantity().doubleValue())
                        .description(w.getDescription())
                        .withdrawalDate(w.getWithdrawalDate())
                        .createdAt(w.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageResponse<>(
                withdrawalPage.getNumber(),
                withdrawalPage.getSize(),
                withdrawalPage.getTotalElements(),
                withdrawalPage.getTotalPages(),
                content
        );
    }
}
