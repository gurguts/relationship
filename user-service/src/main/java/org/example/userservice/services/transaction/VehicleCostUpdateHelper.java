package org.example.userservice.services.transaction;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.userservice.clients.VehicleCostApiClient;
import org.example.userservice.exceptions.transaction.TransactionException;
import org.example.userservice.models.dto.UpdateVehicleCostRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class VehicleCostUpdateHelper {

    private static final String ERROR_CODE_FAILED_TO_UPDATE_VEHICLE_COST = "FAILED_TO_UPDATE_VEHICLE_COST";
    private static final String ERROR_CODE_FAILED_TO_REVERT_VEHICLE_COST = "FAILED_TO_REVERT_VEHICLE_COST";
    private static final String OPERATION_ADD = "add";
    private static final String OPERATION_SUBTRACT = "subtract";

    private final VehicleCostApiClient vehicleCostApiClient;

    public void addVehicleCost(@NonNull Long vehicleId, @NonNull BigDecimal amountEur) {
        try {
            UpdateVehicleCostRequest request = new UpdateVehicleCostRequest();
            request.setAmountEur(amountEur);
            request.setOperation(OPERATION_ADD);
            vehicleCostApiClient.updateVehicleCost(vehicleId, request);
        } catch (Exception e) {
            throw new TransactionException(ERROR_CODE_FAILED_TO_UPDATE_VEHICLE_COST, "Failed to update vehicle cost: " + e.getMessage());
        }
    }

    public void subtractVehicleCost(@NonNull Long vehicleId, @NonNull BigDecimal amountEur) {
        try {
            UpdateVehicleCostRequest request = new UpdateVehicleCostRequest();
            request.setAmountEur(amountEur);
            request.setOperation(OPERATION_SUBTRACT);
            vehicleCostApiClient.updateVehicleCost(vehicleId, request);
        } catch (Exception e) {
            throw new TransactionException(ERROR_CODE_FAILED_TO_REVERT_VEHICLE_COST, "Failed to revert vehicle cost: " + e.getMessage());
        }
    }
}
