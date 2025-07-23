package org.example.clientservice.models.dto.client;

import lombok.Data;
import org.example.clientservice.models.dto.fields.*;

import java.util.List;

@Data
public class ClientListDTO {
    private Long id;
    private String company;
    private String person;
    private String location;
    private String pricePurchase;
    private String priceSale;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
    private String volumeMonth;
    private String comment;
    private Boolean urgently;
    private String edrpou;
    private String enterpriseName;
    private Boolean vat;

    private BusinessDTO business;
    private RouteDTO route;
    private RegionDTO region;
    private StatusDTO status;
    private SourceDTO source;
    private ClientProductDTO clientProduct;

    private List<String> phoneNumbers;
}