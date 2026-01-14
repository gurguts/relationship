package org.example.purchaseservice.services.warehouse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.WithdrawalReasonNotFoundException;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.impl.IWithdrawalReasonService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalReasonService implements IWithdrawalReasonService {
    private final WithdrawalReasonRepository withdrawalReasonRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "withdrawalReasons", key = "#id")
    public WithdrawalReason getWithdrawalReason(@NonNull Long id) {
        return withdrawalReasonRepository.findById(id)
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                        String.format("WithdrawalReason with ID %d not found", id)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "withdrawalReasons", key = "'allWithdrawalReasons'")
    public List<WithdrawalReason> getAllWithdrawalReasons() {
        return withdrawalReasonRepository.findAll();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public WithdrawalReason createWithdrawalReason(@NonNull WithdrawalReason withdrawalReason) {
        log.info("Creating new withdrawal reason: name={}", withdrawalReason.getName());
        validateWithdrawalReason(withdrawalReason);
        WithdrawalReason saved = withdrawalReasonRepository.save(withdrawalReason);
        log.info("Withdrawal reason created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public WithdrawalReason updateWithdrawalReason(@NonNull Long id, @NonNull WithdrawalReason withdrawalReason) {
        log.info("Updating withdrawal reason: id={}", id);
        validateWithdrawalReasonForUpdate(withdrawalReason);
        WithdrawalReason existingWithdrawalReason = withdrawalReasonRepository.findById(id)
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                        String.format("WithdrawalReason with ID %d not found", id)));

        if (withdrawalReason.getName() != null) {
            existingWithdrawalReason.setName(withdrawalReason.getName());
        }
        if (withdrawalReason.getPurpose() != null) {
            existingWithdrawalReason.setPurpose(withdrawalReason.getPurpose());
        }

        WithdrawalReason saved = withdrawalReasonRepository.save(existingWithdrawalReason);
        log.info("Withdrawal reason updated: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public void deleteWithdrawalReason(@NonNull Long id) {
        log.info("Deleting withdrawal reason: id={}", id);
        WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(id)
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                        String.format("WithdrawalReason with ID %d not found", id)));
        withdrawalReasonRepository.delete(withdrawalReason);
        log.info("Withdrawal reason deleted: id={}", id);
    }

    private void validateWithdrawalReason(@NonNull WithdrawalReason withdrawalReason) {
        if (withdrawalReason.getName() == null || withdrawalReason.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("WithdrawalReason name cannot be null or empty");
        }
        if (withdrawalReason.getPurpose() == null) {
            throw new IllegalArgumentException("WithdrawalReason purpose cannot be null");
        }
    }

    private void validateWithdrawalReasonForUpdate(@NonNull WithdrawalReason withdrawalReason) {
        if (withdrawalReason.getName() != null && withdrawalReason.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("WithdrawalReason name cannot be empty");
        }
    }
}
