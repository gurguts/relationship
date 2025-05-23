package org.example.saleservice.mappers;

import org.example.saleservice.exceptions.SaleException;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.fields.SaleCreateDTO;
import org.example.saleservice.models.dto.fields.SaleDTO;
import org.example.saleservice.models.dto.fields.SaleModalDTO;
import org.example.saleservice.models.dto.fields.SaleUpdateDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SaleMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SaleDTO toDto(Sale sale) {
        if (sale == null) {
            return null;
        }
        SaleDTO dto = new SaleDTO();
        dto.setId(sale.getId());
        dto.setUserId(sale.getUser());
        dto.setClientId(sale.getClient());
        dto.setSourceId(sale.getSource());
        dto.setProductId(sale.getProduct());
        dto.setQuantity(sale.getQuantity());
        dto.setUnitPrice(sale.getUnitPrice());
        dto.setTotalPrice(sale.getTotalPrice());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setCurrency(sale.getCurrency());
        dto.setTransactionId(sale.getTransaction());
        dto.setCreatedAt(sale.getCreatedAt());
        dto.setUpdatedAt(sale.getUpdatedAt());
        return dto;
    }

    public Sale saleUpdateDTOToSale(SaleUpdateDTO saleUpdateDTO) {
        if (saleUpdateDTO == null) {
            return null;
        }
        Sale sale = new Sale();
        sale.setSource(saleUpdateDTO.getSourceId());
        sale.setProduct(saleUpdateDTO.getProductId());
        sale.setQuantity(saleUpdateDTO.getQuantity());
        sale.setTotalPrice(saleUpdateDTO.getTotalPrice());
        sale.setCreatedAt(parseCreatedAt(saleUpdateDTO.getCreatedAt()));

        return sale;
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt != null && !createdAt.isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(createdAt, DATE_FORMATTER);
                return localDate.atStartOfDay();
            } catch (Exception e) {
                throw new SaleException(String.format("Invalid date format for createdAt: %s", createdAt));
            }
        }
        return null;
    }

    public Sale saleCreateDTOToSale(SaleCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        Sale sale = new Sale();
        sale.setClient(dto.getClientId());
        sale.setSource(dto.getSourceId());
        sale.setProduct(dto.getProductId());
        sale.setQuantity(dto.getQuantity());
        sale.setTotalPrice(dto.getTotalPrice());
        sale.setPaymentMethod(dto.getPaymentMethod());
        sale.setCurrency(dto.getCurrency());
        return sale;
    }

    public SaleModalDTO saleToSaleModalDTO(Sale sale) {
        if (sale == null) {
            return null;
        }

        SaleModalDTO dto = new SaleModalDTO();
        dto.setId(sale.getId());
        dto.setUserId(sale.getUser());
        dto.setSourceId(sale.getSource());
        dto.setProductId(sale.getProduct());
        dto.setQuantity(sale.getQuantity());
        dto.setUnitPrice(sale.getUnitPrice());
        dto.setTotalPrice(sale.getTotalPrice());
        dto.setPaymentMethod(sale.getPaymentMethod());
        dto.setCurrency(sale.getCurrency());
        dto.setCreatedAt(sale.getCreatedAt());
        return dto;
    }
}
