package org.example.purchaseservice.services.impl;

import org.example.purchaseservice.models.warehouse.WithdrawalReason;

import java.util.List;

public interface IWithdrawalReasonService {
    WithdrawalReason getWithdrawalReason(Long id);

    List<WithdrawalReason> getAllWithdrawalReasons();

    WithdrawalReason createWithdrawalReason(WithdrawalReason withdrawalReason);

    WithdrawalReason updateWithdrawalReason(Long id, WithdrawalReason withdrawalReason);

    void deleteWithdrawalReason(Long id);
}
