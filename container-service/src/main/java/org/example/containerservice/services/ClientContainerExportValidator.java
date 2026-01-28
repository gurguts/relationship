package org.example.containerservice.services;

import lombok.NonNull;
import org.example.containerservice.exceptions.ContainerException;
import org.springframework.stereotype.Service;

@Service
public class ClientContainerExportValidator {

    private static final int MAX_QUERY_LENGTH = 255;
    private static final String ERROR_INVALID_QUERY = "INVALID_QUERY";
    private static final String ERROR_INVALID_FIELDS = "INVALID_FIELDS";
    private static final String MESSAGE_QUERY_TOO_LONG = "Search query cannot exceed 255 characters";
    private static final String MESSAGE_FIELDS_EMPTY = "The list of fields for export cannot be empty";

    public void validateInputs(String query, @NonNull java.util.List<String> selectedFields) {
        validateQuery(query);
        validateSelectedFields(selectedFields);
    }

    public void validateQuery(String query) {
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw new ContainerException(ERROR_INVALID_QUERY, MESSAGE_QUERY_TOO_LONG);
        }
    }

    public void validateSelectedFields(@NonNull java.util.List<String> selectedFields) {
        if (selectedFields.isEmpty()) {
            throw new ContainerException(ERROR_INVALID_FIELDS, MESSAGE_FIELDS_EMPTY);
        }
    }
}
