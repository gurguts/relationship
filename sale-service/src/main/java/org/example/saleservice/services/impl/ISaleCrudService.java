package org.example.saleservice.services.impl;

import org.example.saleservice.models.Sale;

public interface ISaleCrudService {
    Sale createSale(Sale sale);

    Sale updateSale(Long id, Sale updatedSale);

    Sale findSaleById(Long id);

    void deleteSale(Long id);
}
