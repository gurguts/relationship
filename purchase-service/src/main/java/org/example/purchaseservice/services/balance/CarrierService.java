package org.example.purchaseservice.services.balance;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.example.purchaseservice.services.impl.ICarrierService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierService implements ICarrierService {
    private final CarrierRepository carrierRepository;

    @Override
    @Transactional
    public Carrier createCarrier(@NonNull Carrier carrier) {
        if (carrier.getCompanyName() == null || carrier.getCompanyName().trim().isEmpty()) {
            throw new PurchaseException("INVALID_CARRIER_DATA", "Company name is required");
        }

        log.info("Creating new carrier: companyName={}", carrier.getCompanyName());

        if (carrierRepository.existsByCompanyNameIgnoreCase(carrier.getCompanyName())) {
            throw new PurchaseException("CARRIER_ALREADY_EXISTS",
                    String.format("Carrier with company name '%s' already exists", carrier.getCompanyName()));
        }

        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier created: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Carrier updateCarrier(@NonNull Long carrierId, @NonNull Carrier updateData) {
        log.info("Updating carrier: id={}", carrierId);

        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                        String.format("Carrier not found: id=%d", carrierId)));

        if (updateData.getCompanyName() != null) {
            String normalizedCompanyName = updateData.getCompanyName();
            if (normalizedCompanyName.trim().isEmpty()) {
                throw new PurchaseException("INVALID_CARRIER_DATA", "Company name cannot be empty");
            }

            if (!carrier.getCompanyName().equalsIgnoreCase(normalizedCompanyName) &&
                    carrierRepository.existsByCompanyNameIgnoreCase(normalizedCompanyName)) {
                throw new PurchaseException("CARRIER_ALREADY_EXISTS",
                        String.format("Carrier with company name '%s' already exists", normalizedCompanyName));
            }

            carrier.setCompanyName(normalizedCompanyName);
        }

        if (updateData.getRegistrationAddress() != null) {
            carrier.setRegistrationAddress(updateData.getRegistrationAddress());
        }

        if (updateData.getPhoneNumber() != null) {
            carrier.setPhoneNumber(updateData.getPhoneNumber());
        }

        if (updateData.getCode() != null) {
            carrier.setCode(updateData.getCode());
        }

        if (updateData.getAccount() != null) {
            carrier.setAccount(updateData.getAccount());
        }

        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier updated: id={}", saved.getId());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Carrier getCarrier(@NonNull Long carrierId) {
        return carrierRepository.findById(carrierId)
                .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                        String.format("Carrier not found: id=%d", carrierId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Carrier> getAllCarriers() {
        return carrierRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Carrier> searchCarriersByCompanyName(@NonNull String companyName) {
        return carrierRepository.findByCompanyNameContainingIgnoreCase(companyName);
    }

    @Override
    @Transactional
    public void deleteCarrier(@NonNull Long carrierId) {
        log.info("Deleting carrier: id={}", carrierId);

        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                        String.format("Carrier not found: id=%d", carrierId)));

        carrierRepository.delete(carrier);
        log.info("Carrier deleted: id={}", carrierId);
    }
}

