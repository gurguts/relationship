package org.example.purchaseservice.services.purchase;

import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PurchaseSearchValidator {
    
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int MAX_QUERY_LENGTH = 255;
    
    public void validateSearchRequest(@NonNull Pageable pageable, String query) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Page size cannot exceed %d. Requested size: %d", 
                            MAX_PAGE_SIZE, pageable.getPageSize()));
        }
        
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException(
                    String.format("Page number cannot be negative. Requested page: %d", 
                            pageable.getPageNumber()));
        }
        
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Query length cannot exceed %d characters. Query length: %d", 
                            MAX_QUERY_LENGTH, query.length()));
        }
    }
}
