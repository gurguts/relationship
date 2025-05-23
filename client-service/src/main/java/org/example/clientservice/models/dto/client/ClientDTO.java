package org.example.clientservice.models.dto.client;

import lombok.Data;

import java.util.List;

@Data
public class ClientDTO {
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

    private String businessId;
    private String routeId;
    private String regionId;
    private String statusId;
    private String sourceId;
    private String sourceColor;

    private List<String> phoneNumbers;
}