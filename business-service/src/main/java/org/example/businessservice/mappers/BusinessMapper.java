package org.example.businessservice.mappers;

import org.example.businessservice.models.Business;
import org.example.businessservice.models.dto.BusinessCreateDTO;
import org.example.businessservice.models.dto.BusinessDTO;
import org.example.businessservice.models.dto.BusinessUpdateDTO;
import org.springframework.stereotype.Component;

@Component
public class BusinessMapper {

    public Business businessCreateDTOToBusiness(BusinessCreateDTO businessCreateDTO) {
        Business business = new Business();
        business.setName(businessCreateDTO.getName());
        return business;
    }

    public BusinessDTO businessToBusinessDTO(Business business) {
        BusinessDTO businessDTO = new BusinessDTO();
        businessDTO.setId(business.getId());
        businessDTO.setName(business.getName());
        return businessDTO;
    }

    public Business businessUpdateDTOToBusiness(BusinessUpdateDTO businessUpdateDTO) {
        Business business = new Business();
        business.setName(businessUpdateDTO.getName());
        return business;
    }
}
