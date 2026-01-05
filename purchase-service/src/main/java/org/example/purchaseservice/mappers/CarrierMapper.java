package org.example.purchaseservice.mappers;

import lombok.NonNull;
import org.example.purchaseservice.models.balance.Carrier;
import org.example.purchaseservice.models.dto.balance.CarrierCreateDTO;
import org.example.purchaseservice.models.dto.balance.CarrierDetailsDTO;
import org.example.purchaseservice.models.dto.balance.CarrierUpdateDTO;
import org.example.purchaseservice.utils.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class CarrierMapper {

    public CarrierDetailsDTO carrierToCarrierDetailsDTO(@NonNull Carrier carrier) {
        return CarrierDetailsDTO.builder()
                .id(carrier.getId())
                .companyName(carrier.getCompanyName())
                .registrationAddress(carrier.getRegistrationAddress())
                .phoneNumber(carrier.getPhoneNumber())
                .code(carrier.getCode())
                .account(carrier.getAccount())
                .createdAt(carrier.getCreatedAt())
                .updatedAt(carrier.getUpdatedAt())
                .build();
    }

    public Carrier carrierCreateDTOToCarrier(@NonNull CarrierCreateDTO dto) {
        Carrier carrier = new Carrier();
        carrier.setCompanyName(StringUtils.normalizeString(dto.getCompanyName()));
        carrier.setRegistrationAddress(StringUtils.normalizeString(dto.getRegistrationAddress()));
        carrier.setPhoneNumber(StringUtils.normalizeString(dto.getPhoneNumber()));
        carrier.setCode(StringUtils.normalizeString(dto.getCode()));
        carrier.setAccount(StringUtils.normalizeString(dto.getAccount()));
        return carrier;
    }

    public Carrier carrierUpdateDTOToCarrier(@NonNull CarrierUpdateDTO dto) {
        Carrier carrier = new Carrier();
        carrier.setCompanyName(StringUtils.normalizeString(dto.getCompanyName()));
        carrier.setRegistrationAddress(StringUtils.normalizeString(dto.getRegistrationAddress()));
        carrier.setPhoneNumber(StringUtils.normalizeString(dto.getPhoneNumber()));
        carrier.setCode(StringUtils.normalizeString(dto.getCode()));
        carrier.setAccount(StringUtils.normalizeString(dto.getAccount()));
        return carrier;
    }
}
