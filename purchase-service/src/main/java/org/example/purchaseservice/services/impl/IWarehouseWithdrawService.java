package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;

import java.util.List;
import java.util.Map;

public interface IWarehouseWithdrawService {
    WarehouseWithdrawal createWithdrawal(WarehouseWithdrawal request);

    WarehouseWithdrawal updateWithdrawal(Long id, WarehouseWithdrawal request);

    void deleteWithdrawal(Long id);

    PageResponse<WithdrawalDTO> getWithdrawals(int page, int size, String sort, String direction, Map<String, List<String>> filterMap);
}
