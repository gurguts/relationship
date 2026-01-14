package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.Carrier;

import java.util.List;

public interface ICarrierService {
    Carrier createCarrier(@NonNull Carrier carrier);
    
    Carrier updateCarrier(@NonNull Long carrierId, @NonNull Carrier updateData);
    
    Carrier getCarrier(@NonNull Long carrierId);
    
    List<Carrier> getAllCarriers();
    
    List<Carrier> searchCarriersByCompanyName(@NonNull String companyName);
    
    void deleteCarrier(@NonNull Long carrierId);
}
