package org.example.purchaseservice.repositories;

import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalReasonRepository extends JpaRepository<WithdrawalReason, Long> {
    List<WithdrawalReason> findByPurpose(WithdrawalReason.Purpose purpose);
}
