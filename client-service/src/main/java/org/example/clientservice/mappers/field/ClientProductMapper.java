package org.example.clientservice.mappers.field;

import org.example.clientservice.models.dto.fields.*;
import org.example.clientservice.models.field.ClientProduct;
import org.springframework.stereotype.Component;

@Component
public class ClientProductMapper {

    public ClientProductDTO clientProductToClientProductDTO(ClientProduct clientProduct) {
        ClientProductDTO clientProductDTO = new ClientProductDTO();
        clientProductDTO.setId(clientProduct.getId());
        clientProductDTO.setName(clientProduct.getName());
        return clientProductDTO;
    }

    public ClientProduct clientProductCreateDTOToClientProduct(ClientProductCreateDTO dto) {
        ClientProduct clientProduct = new ClientProduct();
        clientProduct.setName(dto.name());
        return clientProduct;
    }


    public ClientProduct clientProductUpdateDTOToClientProduct(ClientProductUpdateDTO dto) {
        ClientProduct clientProduct = new ClientProduct();
        clientProduct.setName(dto.name());
        return clientProduct;
    }
}
