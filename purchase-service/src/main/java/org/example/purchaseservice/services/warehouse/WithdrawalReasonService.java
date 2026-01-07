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
        try {
            return withdrawalReasonRepository.findById(id)
                    .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                            String.format("WithdrawalReason with ID %d not found", id)));
        } catch (WithdrawalReasonNotFoundException e) {
            log.error("WithdrawalReason not found: id={}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Error getting withdrawal reason: id={}", id, e);
            throw new WithdrawalReasonNotFoundException(
                    String.format("WithdrawalReason with ID %d not found", id));
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "withdrawalReasons", key = "'allWithdrawalReasons'")
    public List<WithdrawalReason> getAllWithdrawalReasons() {
        try {
            return withdrawalReasonRepository.findAll();
        } catch (Exception e) {
            log.error("Error getting all withdrawal reasons", e);
            throw new RuntimeException("Failed to get all withdrawal reasons", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public WithdrawalReason createWithdrawalReason(@NonNull WithdrawalReason withdrawalReason) {
        validateWithdrawalReason(withdrawalReason);
        try {
            return withdrawalReasonRepository.save(withdrawalReason);
        } catch (Exception e) {
            log.error("Error creating withdrawal reason: name={}", withdrawalReason.getName(), e);
            throw new RuntimeException("Failed to create withdrawal reason", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public WithdrawalReason updateWithdrawalReason(@NonNull Long id, @NonNull WithdrawalReason withdrawalReason) {
        validateWithdrawalReasonForUpdate(withdrawalReason);
        try {
            WithdrawalReason existingWithdrawalReason = withdrawalReasonRepository.findById(id)
                    .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                            String.format("WithdrawalReason with ID %d not found", id)));
            
            if (withdrawalReason.getName() != null) {
                existingWithdrawalReason.setName(withdrawalReason.getName());
            }
            if (withdrawalReason.getPurpose() != null) {
                existingWithdrawalReason.setPurpose(withdrawalReason.getPurpose());
            }
            
            return withdrawalReasonRepository.save(existingWithdrawalReason);
        } catch (WithdrawalReasonNotFoundException e) {
            log.error("WithdrawalReason not found for update: id={}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Error updating withdrawal reason: id={}", id, e);
            throw new RuntimeException("Failed to update withdrawal reason", e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public void deleteWithdrawalReason(@NonNull Long id) {
        try {
            WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(id)
                    .orElseThrow(() -> new WithdrawalReasonNotFoundException(
                            String.format("WithdrawalReason with ID %d not found", id)));
            withdrawalReasonRepository.delete(withdrawalReason);
        } catch (WithdrawalReasonNotFoundException e) {
            log.error("WithdrawalReason not found for deletion: id={}", id, e);
            throw e;
        } catch (Exception e) {
            log.error("Error deleting withdrawal reason: id={}", id, e);
            throw new RuntimeException("Failed to delete withdrawal reason", e);
        }
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
