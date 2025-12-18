package org.example.purchaseservice.services.balance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.dto.balance.CarrierCreateDTO;
import org.example.purchaseservice.models.dto.balance.CarrierUpdateDTO;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierService {
    
    private final CarrierRepository carrierRepository;
    
    @Transactional
    public Carrier createCarrier(CarrierCreateDTO dto) {
        log.info("Creating new carrier: companyName={}", dto.getCompanyName());
        
        Carrier carrier = new Carrier();
        carrier.setCompanyName(dto.getCompanyName());
        carrier.setRegistrationAddress(dto.getRegistrationAddress());
        carrier.setPhoneNumber(dto.getPhoneNumber());
        carrier.setCode(dto.getCode());
        carrier.setAccount(dto.getAccount());
        
        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier created: id={}", saved.getId());
        
        return saved;
    }
    
    @Transactional
    public Carrier updateCarrier(Long carrierId, CarrierUpdateDTO dto) {
        log.info("Updating carrier: id={}", carrierId);
        
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                        String.format("Carrier not found: id=%d", carrierId)));
        
        if (dto.getCompanyName() != null) {
            carrier.setCompanyName(dto.getCompanyName());
        }
        carrier.setRegistrationAddress(normalizeString(dto.getRegistrationAddress()));
        carrier.setPhoneNumber(normalizeString(dto.getPhoneNumber()));
        carrier.setCode(normalizeString(dto.getCode()));
        carrier.setAccount(normalizeString(dto.getAccount()));
        
        Carrier saved = carrierRepository.save(carrier);
        log.info("Carrier updated: id={}", saved.getId());
        return saved;
    }
    
    @Transactional(readOnly = true)
    public Carrier getCarrier(Long carrierId) {
        return carrierRepository.findById(carrierId)
                .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                        String.format("Carrier not found: id=%d", carrierId)));
    }
    
    @Transactional(readOnly = true)
    public List<Carrier> getAllCarriers() {
        return carrierRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Carrier> searchCarriersByCompanyName(String companyName) {
        return carrierRepository.findByCompanyNameContainingIgnoreCase(companyName);
    }
    
    @Transactional
    public void deleteCarrier(Long carrierId) {
        log.info("Deleting carrier: id={}", carrierId);
        
        Carrier carrier = carrierRepository.findById(carrierId)
                .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                        String.format("Carrier not found: id=%d", carrierId)));
        
        carrierRepository.delete(carrier);
        log.info("Carrier deleted: id={}", carrierId);
    }
    
    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

