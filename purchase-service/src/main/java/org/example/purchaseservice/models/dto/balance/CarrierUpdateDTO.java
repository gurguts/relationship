package org.example.purchaseservice.models.dto.balance;

import lombok.Data;

@Data
public class CarrierUpdateDTO {
    private String companyName;
    private String registrationAddress;
    private String phoneNumber;
    private String code;
    private String account;
}

