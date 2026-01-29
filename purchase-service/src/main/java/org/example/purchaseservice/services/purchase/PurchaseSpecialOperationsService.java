package org.example.purchaseservice.services.purchase;

import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.models.dto.client.ClientDTO;
import org.example.purchaseservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.purchaseservice.models.dto.fields.SourceDTO;
import org.example.purchaseservice.services.impl.IPurchaseSpecialOperationsService;
import org.example.purchaseservice.services.purchase.PurchaseExportDataFetcher.FilterIds;
import org.example.purchaseservice.services.purchase.PurchaseExportDataFetcher.SearchContext;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseSpecialOperationsService implements IPurchaseSpecialOperationsService {

    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    
    private final PurchaseExportValidator validator;
    private final PurchaseExportDataFetcher dataFetcher;
    private final PurchaseExcelGenerator excelGenerator;
    private final PurchaseExportFilenameGenerator filenameGenerator;
    private final PurchaseComparisonReportService comparisonReportService;

    @Override
    @Transactional(readOnly = true)
    public void generateExcelFile(
            Sort.Direction sortDirection,
            @NonNull String sortProperty,
            String query,
            Map<String, List<String>> filterParams,
            @NonNull HttpServletResponse response,
            @NonNull List<String> selectedFields) {

        validator.validateInputs(query, selectedFields);
        validator.validateFilterParams(filterParams);
        validator.validateSortProperty(sortProperty);
        
        Sort sort = Sort.by(sortDirection, sortProperty);
        List<ClientDTO> clients = dataFetcher.fetchClientIds(query, filterParams);
        
        if (clients.isEmpty()) {
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        SearchContext searchContext = dataFetcher.prepareSearchContext(query, filterParams, clients);
        List<org.example.purchaseservice.models.Purchase> purchaseList = dataFetcher.fetchPurchases(
                query, filterParams, searchContext.clientIds(), searchContext.sourceIds(), sort);
        
        FilterIds updatedFilterIds = dataFetcher.buildUpdatedFilterIds(purchaseList, dataFetcher.createFilterIds());
        Map<Long, ClientDTO> clientMap = dataFetcher.fetchClientMap(clients);
        Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap = dataFetcher.fetchClientFieldValues(searchContext.clientIds());
        List<SourceDTO> clientSourceDTOs = dataFetcher.fetchClientSourceDTOs(clients);
        FilterIds filterIdsWithClientSources = new FilterIds(
                dataFetcher.mergeSourceDTOs(updatedFilterIds.sourceDTOs(), clientSourceDTOs),
                updatedFilterIds.sourceIds(),
                updatedFilterIds.productDTOs(),
                updatedFilterIds.productIds(),
                updatedFilterIds.userDTOs(),
                updatedFilterIds.userIds()
        );

        Workbook workbook = excelGenerator.generateWorkbook(purchaseList, selectedFields, filterIdsWithClientSources, 
                clientMap, clientFieldValuesMap);
        sendExcelFileResponse(workbook, response);
    }

    @Override
    public void generateComparisonExcelFile(@NonNull String purchaseDataFrom, @NonNull String purchaseDataTo, 
                                            @NonNull HttpServletResponse response) {
        comparisonReportService.generateComparisonExcelFile(purchaseDataFrom, purchaseDataTo, response);
    }

    private void sendExcelFileResponse(@NonNull Workbook workbook, @NonNull HttpServletResponse response) {
        try {
            response.setContentType(EXCEL_CONTENT_TYPE);
            String filename = filenameGenerator.generateFilename();
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            try {
                workbook.close();
            } catch (IOException closeException) {
                log.error("Failed to close workbook: {}", closeException.getMessage(), closeException);
            }
            throw new PurchaseException("EXCEL_GENERATION_ERROR", 
                String.format("Error generating Excel file: %s", e.getMessage()));
        }
    }
}
