package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class ClientSearchValidator {

    private static final Set<String> VALID_SORT_PROPERTIES = Set.of("company", "source", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT_PROPERTY = "updatedAt";
    private static final Sort.Direction DEFAULT_SORT_DIRECTION = Sort.Direction.DESC;
    private static final int MAX_QUERY_LENGTH = 255;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 1000;
    private static final int MAX_IDS_LIMIT = 1000;

    public void validatePaginationParams(int size, int page) {
        if (size < MIN_PAGE_SIZE) {
            throw new ClientException("INVALID_PAGE_SIZE", 
                    String.format("Page size must be at least %d", MIN_PAGE_SIZE));
        }
        if (size > MAX_PAGE_SIZE) {
            throw new ClientException("INVALID_PAGE_SIZE", 
                    String.format("Page size cannot exceed %d", MAX_PAGE_SIZE));
        }
        if (page < 0) {
            throw new ClientException("INVALID_PAGE_NUMBER", "Page number must be non-negative");
        }
    }

    public void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ClientException("INVALID_QUERY", 
                    String.format("Search query cannot exceed %d characters", MAX_QUERY_LENGTH));
        }
    }

    public void validateIds(@NonNull java.util.List<Long> ids) {
        if (ids.isEmpty()) {
            throw new ClientException("INVALID_IDS", "List of IDs cannot be empty");
        }
        if (ids.size() > MAX_IDS_LIMIT) {
            throw new ClientException("INVALID_IDS", 
                    String.format("Cannot search for more than %d IDs at once", MAX_IDS_LIMIT));
        }
        if (ids.contains(null)) {
            throw new ClientException("INVALID_IDS", "List of IDs cannot contain null values");
        }
    }

    public String validateAndNormalizeSortProperty(String sortProperty) {
        if (sortProperty == null || !VALID_SORT_PROPERTIES.contains(sortProperty)) {
            return DEFAULT_SORT_PROPERTY;
        }
        return sortProperty;
    }

    public Sort.Direction validateSortDirection(String sortProperty, Sort.Direction sortDirection) {
        if (sortProperty == null || !VALID_SORT_PROPERTIES.contains(sortProperty)) {
            return DEFAULT_SORT_DIRECTION;
        }
        return sortDirection != null ? sortDirection : DEFAULT_SORT_DIRECTION;
    }

    public void validateClientIds(@NonNull java.util.List<Long> clientIds) {
        if (clientIds.size() > MAX_IDS_LIMIT) {
            throw new ClientException("TOO_MANY_IDS", 
                    String.format("Cannot request more than %d client IDs at once", MAX_IDS_LIMIT));
        }
    }
}
