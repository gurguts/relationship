package org.example.purchaseservice.models.dto.warehouse;

import java.math.BigDecimal;

public record UpdateWarehouseReceiptDTO(BigDecimal quantity, Long typeId) {
}

