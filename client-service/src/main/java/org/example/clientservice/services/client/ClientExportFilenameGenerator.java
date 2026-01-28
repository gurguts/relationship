package org.example.clientservice.services.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.models.clienttype.ClientType;
import org.example.clientservice.services.impl.IClientTypeService;
import org.example.clientservice.utils.FilenameUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientExportFilenameGenerator {

    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String DEFAULT_CLIENT_TYPE_NAME = "клієнти";
    private static final String FILENAME_TEMPLATE = "клієнти_%s_%s.xlsx";
    private static final String FILTER_KEY_CLIENT_TYPE_ID = "clientTypeId";

    private final IClientTypeService clientTypeService;

    public String generateFilename(Map<String, List<String>> filterParams) {
        String dateTime = LocalDateTime.now().format(FILENAME_DATE_FORMATTER);
        String clientTypeName = extractClientTypeName(filterParams);
        return String.format(FILENAME_TEMPLATE, clientTypeName, dateTime);
    }

    private String extractClientTypeName(Map<String, List<String>> filterParams) {
        Long clientTypeId = extractClientTypeId(filterParams);
        
        if (clientTypeId == null) {
            return DEFAULT_CLIENT_TYPE_NAME;
        }
        
        try {
            ClientType clientType = clientTypeService.getClientTypeById(clientTypeId);
            return FilenameUtils.sanitizeFilename(clientType.getName());
        } catch (Exception e) {
            log.warn("Failed to get client type name for filename generation: {}", e.getMessage());
        }
        
        return DEFAULT_CLIENT_TYPE_NAME;
    }

    private Long extractClientTypeId(Map<String, List<String>> filterParams) {
        if (filterParams == null || !filterParams.containsKey(FILTER_KEY_CLIENT_TYPE_ID)) {
            return null;
        }
        
        List<String> clientTypeIdList = filterParams.get(FILTER_KEY_CLIENT_TYPE_ID);
        if (clientTypeIdList == null || clientTypeIdList.isEmpty()) {
            return null;
        }
        
        try {
            return Long.parseLong(clientTypeIdList.getFirst());
        } catch (NumberFormatException e) {
            log.warn("Invalid clientTypeId format: {}", clientTypeIdList.getFirst());
            return null;
        }
    }
}
