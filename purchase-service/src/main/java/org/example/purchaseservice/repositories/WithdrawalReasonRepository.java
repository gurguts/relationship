package org.example.purchaseservice.repositories;

import lombok.NonNull;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalReasonRepository extends JpaRepository<WithdrawalReason, Long> {
    @NonNull
    List<WithdrawalReason> findByPurpose(@NonNull WithdrawalReason.Purpose purpose);
}
