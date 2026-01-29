package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.*;
import org.example.purchaseservice.models.balance.VehicleTerminal;
import org.example.purchaseservice.models.balance.VehicleDestinationCountry;
import org.example.purchaseservice.models.balance.VehicleDestinationPlace;
import org.example.purchaseservice.models.dto.balance.VehicleUpdateDTO;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.example.purchaseservice.repositories.VehicleReceiverRepository;
import org.example.purchaseservice.repositories.VehicleSenderRepository;
import org.example.purchaseservice.repositories.VehicleTerminalRepository;
import org.example.purchaseservice.repositories.VehicleDestinationCountryRepository;
import org.example.purchaseservice.repositories.VehicleDestinationPlaceRepository;
import org.example.purchaseservice.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VehicleUpdateService {
    
    private final CarrierRepository carrierRepository;
    private final VehicleSenderRepository vehicleSenderRepository;
    private final VehicleReceiverRepository vehicleReceiverRepository;
    private final VehicleTerminalRepository vehicleTerminalRepository;
    private final VehicleDestinationCountryRepository vehicleDestinationCountryRepository;
    private final VehicleDestinationPlaceRepository vehicleDestinationPlaceRepository;
    
    private static final int PRICE_SCALE = 6;
    
    public void updateVehicle(@NonNull Vehicle vehicle, @NonNull VehicleUpdateDTO dto) {
        updateBasicFields(vehicle, dto);
        updateVehicleRelations(vehicle, dto.getSenderId(), dto.getReceiverId(), dto.getCarrierId(), 
                dto.getTerminalId(), dto.getDestinationCountryId(), dto.getDestinationPlaceId());
        updateInvoicePrices(vehicle);
    }
    
    private void updateBasicFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        updateBasicInfoFields(vehicle, dto);
        updateBooleanFields(vehicle, dto);
        updateDateAndPriceFields(vehicle, dto);
    }
    
    private void updateBasicInfoFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        if (dto.getShipmentDate() != null) {
            vehicle.setShipmentDate(dto.getShipmentDate());
        }
        
        vehicle.setVehicleNumber(normalizeString(dto.getVehicleNumber()));
        vehicle.setInvoiceUa(normalizeString(dto.getInvoiceUa()));
        vehicle.setInvoiceEu(normalizeString(dto.getInvoiceEu()));
        vehicle.setDescription(normalizeString(dto.getDescription()));
        vehicle.setProduct(normalizeString(dto.getProduct()));
        vehicle.setProductQuantity(normalizeString(dto.getProductQuantity()));
        vehicle.setDeclarationNumber(normalizeString(dto.getDeclarationNumber()));
        vehicle.setDriverFullName(normalizeString(dto.getDriverFullName()));
    }
    
    private void updateBooleanFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        if (dto.getEur1() != null) {
            vehicle.setEur1(dto.getEur1());
        }
        
        if (dto.getIsOurVehicle() != null) {
            vehicle.setIsOurVehicle(dto.getIsOurVehicle());
        }
        
        if (dto.getFito() != null) {
            vehicle.setFito(dto.getFito());
        }
    }
    
    private void updateDateAndPriceFields(Vehicle vehicle, VehicleUpdateDTO dto) {
        updateCustomsDates(vehicle, dto);
        updateInvoiceDatesAndPrices(vehicle, dto);
        vehicle.setReclamation(dto.getReclamation());
    }
    
    private void updateCustomsDates(Vehicle vehicle, VehicleUpdateDTO dto) {
        vehicle.setCustomsDate(dto.getCustomsDate());
        vehicle.setCustomsClearanceDate(dto.getCustomsClearanceDate());
        vehicle.setUnloadingDate(dto.getUnloadingDate());
    }
    
    private void updateInvoiceDatesAndPrices(Vehicle vehicle, VehicleUpdateDTO dto) {
        vehicle.setInvoiceUaDate(dto.getInvoiceUaDate());
        vehicle.setInvoiceUaPricePerTon(dto.getInvoiceUaPricePerTon());
        vehicle.setInvoiceEuDate(dto.getInvoiceEuDate());
        vehicle.setInvoiceEuPricePerTon(dto.getInvoiceEuPricePerTon());
    }
    
    private String normalizeString(String value) {
        return StringUtils.normalizeString(value);
    }
    
    private void updateVehicleRelations(Vehicle vehicle, Long senderId, Long receiverId, Long carrierId, 
                                       Long terminalId, Long destinationCountryId, Long destinationPlaceId) {
        updateVehicleSender(vehicle, senderId);
        updateVehicleReceiver(vehicle, receiverId);
        updateVehicleCarrier(vehicle, carrierId);
        updateVehicleTerminal(vehicle, terminalId);
        updateVehicleDestinationCountry(vehicle, destinationCountryId);
        updateVehicleDestinationPlace(vehicle, destinationPlaceId);
    }
    
    private void updateVehicleSender(Vehicle vehicle, Long senderId) {
        if (senderId != null) {
            VehicleSender sender = vehicleSenderRepository.findById(senderId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                            String.format("Vehicle sender not found: id=%d", senderId)));
            vehicle.setSender(sender);
        } else {
            vehicle.setSender(null);
        }
    }
    
    private void updateVehicleReceiver(Vehicle vehicle, Long receiverId) {
        if (receiverId != null) {
            VehicleReceiver receiver = vehicleReceiverRepository.findById(receiverId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                            String.format("Vehicle receiver not found: id=%d", receiverId)));
            vehicle.setReceiver(receiver);
        } else {
            vehicle.setReceiver(null);
        }
    }
    
    private void updateVehicleCarrier(Vehicle vehicle, Long carrierId) {
        if (carrierId != null) {
            Carrier carrier = carrierRepository.findById(carrierId)
                    .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                            String.format("Carrier not found: id=%d", carrierId)));
            vehicle.setCarrier(carrier);
        } else if (vehicle.getCarrier() != null) {
            vehicle.setCarrier(null);
        }
    }

    private void updateVehicleTerminal(Vehicle vehicle, Long terminalId) {
        if (terminalId != null) {
            VehicleTerminal terminal = vehicleTerminalRepository.findById(terminalId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_TERMINAL_NOT_FOUND",
                            String.format("Vehicle terminal not found: id=%d", terminalId)));
            vehicle.setTerminal(terminal);
        } else {
            vehicle.setTerminal(null);
        }
    }

    private void updateVehicleDestinationCountry(Vehicle vehicle, Long destinationCountryId) {
        if (destinationCountryId != null) {
            VehicleDestinationCountry country = vehicleDestinationCountryRepository.findById(destinationCountryId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_DESTINATION_COUNTRY_NOT_FOUND",
                            String.format("Vehicle destination country not found: id=%d", destinationCountryId)));
            vehicle.setDestinationCountry(country);
        } else {
            vehicle.setDestinationCountry(null);
        }
    }

    private void updateVehicleDestinationPlace(Vehicle vehicle, Long destinationPlaceId) {
        if (destinationPlaceId != null) {
            VehicleDestinationPlace place = vehicleDestinationPlaceRepository.findById(destinationPlaceId)
                    .orElseThrow(() -> new PurchaseException("VEHICLE_DESTINATION_PLACE_NOT_FOUND",
                            String.format("Vehicle destination place not found: id=%d", destinationPlaceId)));
            vehicle.setDestinationPlace(place);
        } else {
            vehicle.setDestinationPlace(null);
        }
    }
    
    private void updateInvoicePrices(Vehicle vehicle) {
        String productQuantity = vehicle.getProductQuantity();
        BigDecimal quantityInTons = parseProductQuantity(productQuantity);
        
        if (quantityInTons != null) {
            if (vehicle.getInvoiceUaPricePerTon() != null) {
                vehicle.setInvoiceUaTotalPrice(vehicle.getInvoiceUaPricePerTon()
                        .multiply(quantityInTons).setScale(PRICE_SCALE, java.math.RoundingMode.HALF_UP));
            } else {
                vehicle.setInvoiceUaTotalPrice(null);
            }
            
            if (vehicle.getInvoiceEuPricePerTon() != null) {
                vehicle.setInvoiceEuTotalPrice(vehicle.getInvoiceEuPricePerTon()
                        .multiply(quantityInTons).setScale(PRICE_SCALE, java.math.RoundingMode.HALF_UP));
            } else {
                vehicle.setInvoiceEuTotalPrice(null);
            }
        } else {
            vehicle.setInvoiceUaTotalPrice(null);
            vehicle.setInvoiceEuTotalPrice(null);
        }
    }
    
    private BigDecimal parseProductQuantity(String productQuantity) {
        if (productQuantity == null || productQuantity.trim().isEmpty()) {
            return null;
        }
        
        try {
            return new BigDecimal(productQuantity.replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
