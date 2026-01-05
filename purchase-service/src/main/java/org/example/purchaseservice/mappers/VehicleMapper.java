package org.example.purchaseservice.mappers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.balance.Vehicle;
import org.example.purchaseservice.models.balance.VehicleExpense;
import org.example.purchaseservice.models.balance.VehicleProduct;
import org.example.purchaseservice.models.balance.VehicleReceiver;
import org.example.purchaseservice.models.balance.VehicleSender;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.VehicleCreateDTO;
import org.example.purchaseservice.models.dto.balance.VehicleDetailsDTO;
import org.example.purchaseservice.repositories.CarrierRepository;
import org.example.purchaseservice.repositories.VehicleReceiverRepository;
import org.example.purchaseservice.repositories.VehicleSenderRepository;
import org.example.purchaseservice.services.balance.VehicleExpenseService;
import org.example.purchaseservice.services.balance.VehicleService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VehicleMapper {
    private final CarrierMapper carrierMapper;
    private final VehicleService vehicleService;
    private final VehicleExpenseService vehicleExpenseService;
    private final CarrierRepository carrierRepository;
    private final VehicleSenderRepository vehicleSenderRepository;
    private final VehicleReceiverRepository vehicleReceiverRepository;

    public VehicleDetailsDTO vehicleToVehicleDetailsDTO(@NonNull Vehicle vehicle) {
        List<VehicleProduct> products = vehicleService.getVehicleProducts(vehicle.getId());
        BigDecimal expensesTotal = calculateExpensesTotal(vehicle.getId());
        return vehicleToVehicleDetailsDTO(vehicle, products, expensesTotal);
    }

    public VehicleDetailsDTO vehicleToVehicleDetailsDTO(@NonNull Vehicle vehicle,
                                                         @NonNull List<VehicleProduct> products,
                                                         @NonNull BigDecimal expensesTotal) {
        List<VehicleDetailsDTO.VehicleItemDTO> items = products.stream()
                .map(p -> VehicleDetailsDTO.VehicleItemDTO.builder()
                        .withdrawalId(p.getId())
                        .productId(p.getProductId())
                        .productName(null)
                        .warehouseId(p.getWarehouseId())
                        .quantity(p.getQuantity())
                        .unitPriceEur(p.getUnitPriceEur())
                        .totalCostEur(p.getTotalCostEur())
                        .withdrawalDate(p.getAddedAt() != null ? p.getAddedAt().toLocalDate() : vehicle.getShipmentDate())
                        .build())
                .toList();

        CarrierDetailsDTO carrierDTO = null;
        if (vehicle.getCarrier() != null) {
            carrierDTO = carrierMapper.carrierToCarrierDetailsDTO(vehicle.getCarrier());
        }

        return VehicleDetailsDTO.builder()
                .id(vehicle.getId())
                .shipmentDate(vehicle.getShipmentDate())
                .vehicleNumber(vehicle.getVehicleNumber())
                .invoiceUa(vehicle.getInvoiceUa())
                .invoiceEu(vehicle.getInvoiceEu())
                .description(vehicle.getDescription())
                .totalCostEur(vehicle.getTotalCostEur())
                .userId(vehicle.getUserId())
                .createdAt(vehicle.getCreatedAt())
                .senderId(vehicle.getSender() != null ? vehicle.getSender().getId() : null)
                .senderName(vehicle.getSender() != null ? vehicle.getSender().getName() : null)
                .receiverId(vehicle.getReceiver() != null ? vehicle.getReceiver().getId() : null)
                .receiverName(vehicle.getReceiver() != null ? vehicle.getReceiver().getName() : null)
                .destinationCountry(vehicle.getDestinationCountry())
                .destinationPlace(vehicle.getDestinationPlace())
                .product(vehicle.getProduct())
                .productQuantity(vehicle.getProductQuantity())
                .declarationNumber(vehicle.getDeclarationNumber())
                .terminal(vehicle.getTerminal())
                .driverFullName(vehicle.getDriverFullName())
                .isOurVehicle(vehicle.getIsOurVehicle())
                .eur1(vehicle.getEur1())
                .fito(vehicle.getFito())
                .customsDate(vehicle.getCustomsDate())
                .customsClearanceDate(vehicle.getCustomsClearanceDate())
                .unloadingDate(vehicle.getUnloadingDate())
                .invoiceUaDate(vehicle.getInvoiceUaDate())
                .invoiceUaPricePerTon(vehicle.getInvoiceUaPricePerTon())
                .invoiceUaTotalPrice(vehicle.getInvoiceUaTotalPrice())
                .invoiceEuDate(vehicle.getInvoiceEuDate())
                .invoiceEuPricePerTon(vehicle.getInvoiceEuPricePerTon())
                .invoiceEuTotalPrice(vehicle.getInvoiceEuTotalPrice())
                .reclamation(vehicle.getReclamation())
                .totalExpenses(vehicleService.calculateTotalExpenses(products, expensesTotal))
                .totalIncome(vehicleService.calculateTotalIncome(vehicle))
                .margin(vehicleService.calculateMargin(vehicle, products, expensesTotal))
                .carrier(carrierDTO)
                .items(items)
                .build();
    }

    public Vehicle vehicleCreateDTOToVehicle(@NonNull VehicleCreateDTO dto, @NonNull Long userId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setShipmentDate(dto.getShipmentDate() != null ? dto.getShipmentDate() : LocalDate.now());
        vehicle.setVehicleNumber(dto.getVehicleNumber());
        vehicle.setInvoiceUa(dto.getInvoiceUa());
        vehicle.setInvoiceEu(dto.getInvoiceEu());
        vehicle.setDescription(dto.getDescription());
        vehicle.setUserId(userId);
        vehicle.setIsOurVehicle(dto.getIsOurVehicle() != null ? dto.getIsOurVehicle() : false);
        vehicle.setTotalCostEur(BigDecimal.ZERO);
        if (dto.getSenderId() != null) {
            VehicleSender sender = vehicleSenderRepository.findById(dto.getSenderId())
                    .orElseThrow(() -> new PurchaseException("VEHICLE_SENDER_NOT_FOUND",
                            String.format("Vehicle sender not found: id=%d", dto.getSenderId())));
            vehicle.setSender(sender);
        }
        if (dto.getReceiverId() != null) {
            VehicleReceiver receiver = vehicleReceiverRepository.findById(dto.getReceiverId())
                    .orElseThrow(() -> new PurchaseException("VEHICLE_RECEIVER_NOT_FOUND",
                            String.format("Vehicle receiver not found: id=%d", dto.getReceiverId())));
            vehicle.setReceiver(receiver);
        }
        vehicle.setDestinationCountry(normalizeString(dto.getDestinationCountry()));
        vehicle.setDestinationPlace(normalizeString(dto.getDestinationPlace()));
        vehicle.setProduct(normalizeString(dto.getProduct()));
        vehicle.setProductQuantity(normalizeString(dto.getProductQuantity()));
        vehicle.setDeclarationNumber(normalizeString(dto.getDeclarationNumber()));
        vehicle.setTerminal(normalizeString(dto.getTerminal()));
        vehicle.setDriverFullName(normalizeString(dto.getDriverFullName()));
        vehicle.setEur1(dto.getEur1() != null ? dto.getEur1() : false);
        vehicle.setFito(dto.getFito() != null ? dto.getFito() : false);
        vehicle.setCustomsDate(dto.getCustomsDate());
        vehicle.setCustomsClearanceDate(dto.getCustomsClearanceDate());
        vehicle.setUnloadingDate(dto.getUnloadingDate());
        vehicle.setInvoiceUaDate(dto.getInvoiceUaDate());
        vehicle.setInvoiceUaPricePerTon(dto.getInvoiceUaPricePerTon());
        vehicle.setInvoiceEuDate(dto.getInvoiceEuDate());
        vehicle.setInvoiceEuPricePerTon(dto.getInvoiceEuPricePerTon());
        vehicle.setReclamation(dto.getReclamation());
        if (dto.getInvoiceUaPricePerTon() != null && dto.getProductQuantity() != null) {
            try {
                BigDecimal quantityInTons = new BigDecimal(dto.getProductQuantity().replace(",", "."));
                vehicle.setInvoiceUaTotalPrice(dto.getInvoiceUaPricePerTon().multiply(quantityInTons)
                        .setScale(6, java.math.RoundingMode.HALF_UP));
            } catch (NumberFormatException e) {
                vehicle.setInvoiceUaTotalPrice(null);
            }
        }
        if (dto.getInvoiceEuPricePerTon() != null && dto.getProductQuantity() != null) {
            try {
                BigDecimal quantityInTons = new BigDecimal(dto.getProductQuantity().replace(",", "."));
                vehicle.setInvoiceEuTotalPrice(dto.getInvoiceEuPricePerTon().multiply(quantityInTons)
                        .setScale(6, java.math.RoundingMode.HALF_UP));
            } catch (NumberFormatException e) {
                vehicle.setInvoiceEuTotalPrice(null);
            }
        }
        if (dto.getCarrierId() != null) {
            Carrier carrier = carrierRepository.findById(dto.getCarrierId())
                    .orElseThrow(() -> new PurchaseException("CARRIER_NOT_FOUND",
                            String.format("Carrier not found: id=%d", dto.getCarrierId())));
            vehicle.setCarrier(carrier);
        }
        return vehicle;
    }

    private String normalizeString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal calculateExpensesTotal(@NonNull Long vehicleId) {
        BigDecimal expensesTotal = BigDecimal.ZERO;
        List<VehicleExpense> expenses = vehicleExpenseService.getExpensesByVehicleId(vehicleId);
        if (expenses != null && !expenses.isEmpty()) {
            for (VehicleExpense expense : expenses) {
                if (expense.getConvertedAmount() != null) {
                    expensesTotal = expensesTotal.add(expense.getConvertedAmount());
                }
            }
        }
        return expensesTotal;
    }
}

