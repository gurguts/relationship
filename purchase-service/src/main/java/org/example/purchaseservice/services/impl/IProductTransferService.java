package org.example.purchaseservice.services.impl;

import lombok.NonNull;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferResponseDTO;
import org.example.purchaseservice.models.dto.warehouse.ProductTransferUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.TransferExcelData;
import org.example.purchaseservice.models.warehouse.ProductTransfer;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface IProductTransferService {
    ProductTransfer transferProduct(@NonNull ProductTransferDTO transferDTO);
    
    Page<ProductTransferResponseDTO> getTransfers(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId,
            int page,
            int size,
            String sortBy,
            String sortDirection);
    
    List<ProductTransferResponseDTO> getAllTransfers(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId);
    
    ProductTransferResponseDTO updateTransfer(@NonNull Long transferId, @NonNull ProductTransferUpdateDTO updateDTO);
    
    ProductTransferResponseDTO toDTO(@NonNull ProductTransfer transfer);
    
    List<TransferExcelData> getTransfersForExcel(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId);
    
    byte[] exportToExcel(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long warehouseId,
            Long fromProductId,
            Long toProductId,
            Long userId,
            Long reasonId) throws IOException;
}
