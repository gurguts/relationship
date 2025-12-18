package org.example.purchaseservice.services.warehouse;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.WithdrawalReasonNotFoundException;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.example.purchaseservice.repositories.WithdrawalReasonRepository;
import org.example.purchaseservice.services.impl.IWithdrawalReasonService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WithdrawalReasonService implements IWithdrawalReasonService {
    private final WithdrawalReasonRepository withdrawalReasonRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "withdrawalReasons", key = "#id")
    public WithdrawalReason getWithdrawalReason(Long id) {
        return withdrawalReasonRepository.findById(id)
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(String.format("WithdrawalReason with ID %d not found", id)));
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
    public WithdrawalReason createWithdrawalReason(WithdrawalReason withdrawalReason) {
        return withdrawalReasonRepository.save(withdrawalReason);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public WithdrawalReason updateWithdrawalReason(Long id, WithdrawalReason withdrawalReason) {
        WithdrawalReason existingWithdrawalReason = withdrawalReasonRepository.findById(id)
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(String.format("WithdrawalReason with ID %d not found", id)));
        existingWithdrawalReason.setName(withdrawalReason.getName());
        existingWithdrawalReason.setPurpose(withdrawalReason.getPurpose());
        return withdrawalReasonRepository.save(existingWithdrawalReason);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"withdrawalReasons"}, allEntries = true)
    public void deleteWithdrawalReason(Long id) {
        WithdrawalReason withdrawalReason = withdrawalReasonRepository.findById(id)
                .orElseThrow(() -> new WithdrawalReasonNotFoundException(String.format("WithdrawalReason with ID %d not found", id)));
        withdrawalReasonRepository.delete(withdrawalReason);
    }
}
