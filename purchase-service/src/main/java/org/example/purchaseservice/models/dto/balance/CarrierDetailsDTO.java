package org.example.purchaseservice.models.dto.balance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CarrierDetailsDTO {
    private Long id;
    private String companyName;
    private String registrationAddress;
    private String phoneNumber;
    private String code;
    private String account;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

