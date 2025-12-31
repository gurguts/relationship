package org.example.clientservice.restControllers.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.models.dto.client.ClientExportRequest;
import org.example.clientservice.models.dto.client.ClientExportResult;
import org.example.clientservice.services.impl.IClientSpecialOperationsService;
import org.example.clientservice.utils.FilenameUtils;
import org.example.clientservice.utils.JsonUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Validated
public class ClientSpecialOperationsController {
    private final IClientSpecialOperationsService clientService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAuthority('client:excel')")
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportClientToExcel(
            @RequestBody @Valid @NonNull ClientExportRequest request,
            @RequestParam(name = "q", required = false)
            @Size(max = 255, message = "{validation.query.size}") String query,
            @RequestParam(name = "sort", defaultValue = "updatedAt")
            @Pattern(regexp = "^(id|company|createdAt|updatedAt|source)$", message = "{validation.sort.property}")
            String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC")
            Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson
    ) {
        String normalizedQuery = query != null ? query.trim() : null;

        Map<String, List<String>> filters = JsonUtils.parseFilters(objectMapper, filtersJson);

        ClientExportResult result = clientService.exportClientsToExcel(
                sortDirection, sortProperty, normalizedQuery, filters, request.fields());

        String filename = result.filename();
        String encodedFilename = FilenameUtils.encodeFilenameForRfc5987(filename);
        String asciiFilename = FilenameUtils.sanitizeToAscii(filename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s", asciiFilename, encodedFilename));

        return ResponseEntity.ok().headers(headers).body(result.excelData());
    }
}
