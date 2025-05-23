package org.example.clientservice.services.impl;

import org.example.clientservice.models.field.Business;

import java.util.List;
import java.util.Map;

public interface IBusinessService {
    Business getBusiness(Long id);

    List<Business> getAllBusinesses();

    Business createBusiness(Business business);

    Business updateBusiness(Long id, Business business);

    void deleteBusiness(Long id);

    Map<Long, String> getBusinessNames();

    List<Business> findByNameContaining(String query);
}
