package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalRequestDTO;

import java.util.List;
import java.util.Map;

public interface IWarehouseWithdrawService {
    WarehouseWithdrawal createWithdrawal(WithdrawalRequestDTO request);

    WarehouseWithdrawal updateWithdrawal(Long id, WarehouseWithdrawalUpdateDTO request);

    void deleteWithdrawal(Long id);

    PageResponse<WithdrawalDTO> getWithdrawals(int page, int size, String sort, String direction, Map<String, List<String>> filterMap);
}
