package org.example.saleservice.services.impl;

import org.example.saleservice.models.PageResponse;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.fields.SalePageDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ISaleSearchService {
    PageResponse<SalePageDTO> searchSale(String query, Pageable pageable, Map<String, List<String>> filterParams);

    List<Sale> getSalesByClientId(Long clientId);
}
