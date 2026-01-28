package org.example.containerservice.services;

import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.containerservice.exceptions.ContainerException;
import org.example.containerservice.models.ClientContainer;
import org.example.containerservice.models.dto.client.ClientDTO;
import org.example.containerservice.models.dto.clienttype.ClientFieldValueDTO;
import org.example.containerservice.services.ClientContainerExportDataFetcher.FilterIds;
import org.example.containerservice.services.impl.IClientContainerSpecialOperationsService;
import org.example.containerservice.utils.FilterUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientContainerSpecialOperationsService implements IClientContainerSpecialOperationsService {

    private static final String ERROR_EXCEL_GENERATION_ERROR = "EXCEL_GENERATION_ERROR";
    private static final String MESSAGE_EXCEL_ERROR = "Error generating Excel file: %s";
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CONTENT_DISPOSITION_TEMPLATE = "attachment; filename=%s";

    private final ClientContainerExportValidator validator;
    private final ClientContainerExportDataFetcher dataFetcher;
    private final ClientContainerExcelGenerator excelGenerator;
    private final ClientContainerExportFilenameGenerator filenameGenerator;

    @Override
    @Transactional(readOnly = true)
    public void generateExcelFile(Sort.Direction sortDirection,
                                  String sortProperty,
                                  String query,
                                  Map<String, List<String>> filterParams,
                                  @NonNull HttpServletResponse response,
                                  @NonNull List<String> selectedFields) {
        validator.validateInputs(query, selectedFields);

        Sort sort = Sort.by(sortDirection, sortProperty);
        FilterIds filterIds = dataFetcher.fetchFilterIds();

        List<ClientDTO> clients = dataFetcher.fetchClientIds(query, filterParams);
        if (clients.isEmpty()) {
            Workbook workbook = new XSSFWorkbook();
            sendExcelFileResponse(workbook, response);
            return;
        }

        List<Long> clientIds = clients.stream()
                .map(ClientDTO::getId)
                .filter(Objects::nonNull)
                .toList();
        List<ClientContainer> clientContainerList = dataFetcher.fetchClientContainers(query, filterParams, clientIds, sort);
        Map<Long, ClientDTO> clientMap = dataFetcher.fetchClientMap(clients);

        FilterUtils.extractClientTypeId(filterParams);
        Map<Long, List<ClientFieldValueDTO>> clientFieldValuesMap = dataFetcher.fetchClientFieldValues(clientIds);
        List<org.example.containerservice.models.dto.fields.SourceDTO> sourceDTOs = dataFetcher.fetchClientSourceDTOs(clients);

        FilterIds updatedFilterIds = new FilterIds(
                filterIds.userDTOs(),
                filterIds.userIds(),
                sourceDTOs
        );

        Workbook workbook = excelGenerator.generateWorkbook(clientContainerList, selectedFields, updatedFilterIds, clientMap,
                clientFieldValuesMap);

        sendExcelFileResponse(workbook, response);
    }

    private void sendExcelFileResponse(@NonNull Workbook workbook, @NonNull HttpServletResponse response) {
        try {
            response.setContentType(CONTENT_TYPE);
            String filename = filenameGenerator.generateFilename();
            response.setHeader("Content-Disposition", String.format(CONTENT_DISPOSITION_TEMPLATE, filename));
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            try {
                workbook.close();
            } catch (IOException closeException) {
                log.warn("Failed to close workbook: {}", closeException.getMessage());
            }
            throw new ContainerException(ERROR_EXCEL_GENERATION_ERROR,
                    String.format(MESSAGE_EXCEL_ERROR, e.getMessage()));
        }
    }
}

