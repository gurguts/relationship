package org.example.purchaseservice.models.dto.balance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CarrierCreateDTO {
    
    @NotBlank(message = "Company name is required")
    private String companyName;
    
    private String registrationAddress;
    
    private String phoneNumber;
    
    private String code;
    
    private String account;
}

