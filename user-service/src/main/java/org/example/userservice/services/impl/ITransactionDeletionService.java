package org.example.userservice.services.impl;

import lombok.NonNull;

public interface ITransactionDeletionService {
    void deleteTransaction(@NonNull Long transactionId);
    
    void deleteTransactionsByVehicleId(@NonNull Long vehicleId);
}
