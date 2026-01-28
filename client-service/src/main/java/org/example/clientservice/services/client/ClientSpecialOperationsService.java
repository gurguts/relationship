package org.example.clientservice.services.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.models.client.Client;
import org.example.clientservice.models.client.ClientFilterIds;
import org.example.clientservice.models.dto.client.ClientExportResult;
import org.example.clientservice.services.impl.IClientSpecialOperationsService;
import org.example.clientservice.spec.ClientSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientSpecialOperationsService implements IClientSpecialOperationsService {

    private final ClientExportValidator validator;
    private final ClientSearchQueryProcessor queryProcessor;
    private final ClientExportDataFetcher dataFetcher;
    private final ClientExcelGenerator excelGenerator;
    private final ClientExportFilenameGenerator filenameGenerator;

    @Override
    @Transactional(readOnly = true)
    @NonNull
    public ClientExportResult exportClientsToExcel(
            Sort.Direction sortDirection,
            String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            @NonNull List<String> selectedFields
    ) {
        validator.validateQuery(query);
        validator.validateSelectedFields(selectedFields);

        Sort sort = Sort.by(sortDirection, sortProperty);
        String normalizedQuery = queryProcessor.normalizeQuery(query);
        ClientFilterIds filterIds = normalizedQuery != null 
                ? queryProcessor.fetchFilterIds(normalizedQuery) 
                : new ClientFilterIds(Collections.emptyList(), Collections.emptyList());

        List<Client> clientList = fetchClients(normalizedQuery, filterParams, filterIds, sort);

        Workbook workbook = excelGenerator.generateWorkbook(clientList, selectedFields);

        byte[] excelData = convertWorkbookToBytes(workbook);
        String filename = filenameGenerator.generateFilename(filterParams);

        return new ClientExportResult(excelData, filename);
    }


    private List<Client> fetchClients(String normalizedQuery, Map<String, List<String>> filterParams, 
                                     ClientFilterIds filterIds, @NonNull Sort sort) {
        Long clientTypeId = extractClientTypeId(filterParams);
        List<Long> sourceIdsForSpec = normalizedQuery != null ? filterIds.sourceIds() : null;
        
        Specification<Client> spec = new ClientSpecification(normalizedQuery, filterParams, 
                sourceIdsForSpec, clientTypeId);
        
        return dataFetcher.fetchClients(spec, sort);
    }

    private Long extractClientTypeId(Map<String, List<String>> filterParams) {
        if (filterParams == null || !filterParams.containsKey("clientTypeId")) {
            return null;
        }
        
        List<String> clientTypeIdList = filterParams.get("clientTypeId");
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


    private byte[] convertWorkbookToBytes(@NonNull Workbook workbook) {
        byte[] result = new byte[0];
        try (workbook; ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try {
                workbook.write(baos);
                result = baos.toByteArray();
            } catch (IOException e) {
                log.error("Error generating Excel file: {}", e.getMessage(), e);
                throw new ClientException("EXCEL_GENERATION_ERROR",
                        String.format("Error generating Excel file: %s", e.getMessage()));
            }
        } catch (IOException e) {
            log.warn("Failed to close workbook: {}", e.getMessage());
        }
        return result;
    }

}
