package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleExpenseUpdateDTO;

import java.util.List;
import java.util.Map;

public interface IVehicleExpenseService {
    VehicleExpense createVehicleExpense(@NonNull VehicleExpenseCreateDTO dto);
    
    List<VehicleExpense> getExpensesByVehicleId(@NonNull Long vehicleId);
    
    Map<Long, List<VehicleExpense>> getExpensesByVehicleIds(@NonNull List<Long> vehicleIds);
    
    VehicleExpense getExpenseById(@NonNull Long expenseId);
    
    VehicleExpense updateVehicleExpense(@NonNull Long expenseId, @NonNull VehicleExpenseUpdateDTO dto);
    
    void deleteVehicleExpense(@NonNull Long expenseId);
}
