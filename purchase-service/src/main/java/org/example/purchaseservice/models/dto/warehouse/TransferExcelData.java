package org.example.purchaseservice.models.dto.warehouse;

public record TransferExcelData(
        ProductTransferResponseDTO transfer,
        String warehouseName,
        String fromProductName,
        String toProductName,
        String reasonName
) {}

