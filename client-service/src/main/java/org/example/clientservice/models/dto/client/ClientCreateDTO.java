package org.example.clientservice.models.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.clientservice.models.dto.clienttype.ClientFieldValueCreateDTO;

import java.util.List;

@Data
public class ClientCreateDTO {

    @NotNull(message = "{validation.clientTypeId.notnull}")
    private Long clientTypeId;

    @NotBlank(message = "{validation.company.notblank}")
    @Size(max = 255, message = "{validation.company.size}")
    private String company;

    @Size(max = 255, message = "{validation.person.size}")
    private String person;

    private List<String> phoneNumbers;

    @Size(max = 255, message = "{validation.location.size}")
    private String location;

    @Size(max = 255, message = "{validation.pricePurchase.size}")
    private String pricePurchase;

    @Size(max = 255, message = "{validation.priceSale.size}")
    private String priceSale;

    @Size(max = 500, message = "{validation.comment.size}")
    private String comment;

    @Size(max = 255, message = "{validation.volumeMonth.size}")
    private String volumeMonth;

    private Long routeId;

    private Long regionId;

    private Long statusId;

    private Long sourceId;

    private Long businessId;

    private Long clientProductId;

    private List<ClientFieldValueCreateDTO> fieldValues;
}