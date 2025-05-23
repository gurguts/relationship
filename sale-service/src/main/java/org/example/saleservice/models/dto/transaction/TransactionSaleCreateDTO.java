package org.example.saleservice.models.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionSaleCreateDTO {
    private Long targetUserId;
    private Long executorUserId;
    private Long clientId;
    private BigDecimal totalPrice;
    private Long productId;
    private String currency;
}
