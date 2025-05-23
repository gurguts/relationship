package org.example.clientservice.mappers.field;

import org.example.clientservice.models.dto.fields.BusinessCreateDTO;
import org.example.clientservice.models.dto.fields.BusinessDTO;
import org.example.clientservice.models.dto.fields.BusinessUpdateDTO;
import org.example.clientservice.models.field.Business;
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
