package org.example.userservice.services.transaction;

import org.example.userservice.exceptions.transaction.TransactionException;
import org.springframework.stereotype.Component;

@Component
public class TransactionSearchValidator {

    private static final String ERROR_CODE_INVALID_PAGE = "INVALID_PAGE";
    private static final String ERROR_CODE_INVALID_PAGE_SIZE = "INVALID_PAGE_SIZE";
    private static final String ERROR_CODE_INVALID_SORT_DIRECTION = "INVALID_SORT_DIRECTION";
    private static final int MAX_PAGE_SIZE = 1000;

    public void validateSearchParameters(int page, int size, String sort, String direction) {
        if (page < 0) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE,
                    String.format("Page number cannot be negative. Requested page: %d", page));
        }

        if (size <= 0) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE_SIZE,
                    String.format("Page size must be positive. Requested size: %d", size));
        }

        if (size > MAX_PAGE_SIZE) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE_SIZE,
                    String.format("Page size cannot exceed %d. Requested size: %d", MAX_PAGE_SIZE, size));
        }

        if (sort == null || sort.trim().isEmpty()) {
            throw new TransactionException(ERROR_CODE_INVALID_PAGE, "Sort field cannot be null or empty");
        }

        if (direction != null && !direction.equalsIgnoreCase("ASC") && !direction.equalsIgnoreCase("DESC")) {
            throw new TransactionException(ERROR_CODE_INVALID_SORT_DIRECTION,
                    String.format("Invalid sort direction: %s. Must be ASC or DESC", direction));
        }
    }
}
