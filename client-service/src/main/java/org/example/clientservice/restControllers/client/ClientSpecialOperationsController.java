package org.example.clientservice.restControllers.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.exceptions.client.ClientException;
import org.example.clientservice.services.impl.IClientSpecialOperationsService;
import org.example.clientservice.services.clienttype.ClientTypeService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/client")
@RequiredArgsConstructor
@Slf4j
public class ClientSpecialOperationsController {
    private final IClientSpecialOperationsService clientService;
    private final ObjectMapper objectMapper;
    private final ClientTypeService clientTypeService;
    
    private static final DateTimeFormatter FILENAME_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    @PreAuthorize("hasAuthority('client:excel')")
    @PostMapping("/export/excel")
    public ResponseEntity<byte[]> exportClientToExcel(
            @RequestBody Map<String, List<String>> requestBody,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "sort", defaultValue = "updatedAt") String sortProperty,
            @RequestParam(name = "direction", defaultValue = "DESC") Sort.Direction sortDirection,
            @RequestParam(name = "filters", required = false) String filtersJson
    ) {
        List<String> selectedFields = requestBody.get("fields");
        if (selectedFields == null || selectedFields.isEmpty()) {
            throw new ClientException("INVALID_FIELDS", "The list of fields for export cannot be empty");
        }

        if (query != null) {
            query = query.trim();
        }

        Map<String, List<String>> filters = parseFilters(filtersJson);

        byte[] excelData = clientService.generateExcelFile(
                sortDirection, sortProperty, query, filters, selectedFields);

        String filename = generateFilename(filters);

        String encodedFilename = encodeFilenameForRfc5987(filename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        String asciiFilename = sanitizeToAscii(filename);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                String.format("attachment; filename=\"%s\"; filename*=UTF-8''%s", asciiFilename, encodedFilename));

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ClientException("INVALID_JSON", String.format("Invalid JSON format for filters: %s",
                    e.getMessage()));
        }
    }
    
    private String generateFilename(Map<String, List<String>> filters) {
        String dateTime = LocalDateTime.now().format(FILENAME_DATE_FORMATTER);

        String clientTypeName = "клієнти";
        if (filters != null && filters.containsKey("clientTypeId") && !filters.get("clientTypeId").isEmpty()) {
            try {
                Long clientTypeId = Long.parseLong(filters.get("clientTypeId").get(0));
                var clientType = clientTypeService.getClientTypeById(clientTypeId);
                if (clientType != null && clientType.getName() != null) {
                    clientTypeName = sanitizeFilename(clientType.getName());
                    log.debug("Client type name for filename: {}", clientTypeName);
                }
            } catch (Exception e) {
                log.warn("Failed to get client type name for filename: {}", e.getMessage());
            }
        } else {
            log.debug("No clientTypeId in filters, using default name");
        }

        String filename = String.format("клієнти_%s_%s.xlsx", clientTypeName, dateTime);
        log.debug("Generated filename: {}", filename);
        return filename;
    }
    
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
    }
    
    private String sanitizeToAscii(String filename) {
        StringBuilder result = new StringBuilder();
        for (char c : filename.toCharArray()) {
            if (c < 128) {
                if ((c >= '0' && c <= '9') || 
                    (c >= 'A' && c <= 'Z') || 
                    (c >= 'a' && c <= 'z') ||
                    c == '-' || c == '_' || c == '.') {
                    result.append(c);
                } else {
                    result.append('_');
                }
            } else {
                String transliterated = transliterateCyrillic(c);
                if (transliterated.isEmpty() || transliterated.equals("_")) {
                    result.append('_');
                } else {
                    result.append(transliterated);
                }
            }
        }

        String resultStr = result.toString().replaceAll("_{2,}", "_");
        log.debug("Transliterated filename: {} -> {}", filename, resultStr);
        return resultStr;
    }
    
    private String transliterateCyrillic(char c) {
        return switch (c) {
            case 'а', 'А' -> "a";
            case 'б', 'Б' -> "b";
            case 'в', 'В' -> "v";
            case 'г', 'Г' -> "g";
            case 'д', 'Д' -> "d";
            case 'е', 'Е' -> "e";
            case 'є', 'Є' -> "ye";
            case 'ж', 'Ж' -> "zh";
            case 'з', 'З' -> "z";
            case 'и', 'И' -> "y";
            case 'і', 'І' -> "i";
            case 'ї', 'Ї' -> "yi";
            case 'й', 'Й' -> "y";
            case 'к', 'К' -> "k";
            case 'л', 'Л' -> "l";
            case 'м', 'М' -> "m";
            case 'н', 'Н' -> "n";
            case 'о', 'О' -> "o";
            case 'п', 'П' -> "p";
            case 'р', 'Р' -> "r";
            case 'с', 'С' -> "s";
            case 'т', 'Т' -> "t";
            case 'у', 'У' -> "u";
            case 'ф', 'Ф' -> "f";
            case 'х', 'Х' -> "kh";
            case 'ц', 'Ц' -> "ts";
            case 'ч', 'Ч' -> "ch";
            case 'ш', 'Ш' -> "sh";
            case 'щ', 'Щ' -> "shch";
            case 'ь', 'Ь' -> "";
            case 'ю', 'Ю' -> "yu";
            case 'я', 'Я' -> "ya";
            case 'ъ', 'Ъ' -> "";
            default -> "_";
        };
    }
    
    private String encodeFilenameForRfc5987(String filename) {
        StringBuilder encoded = new StringBuilder();
        byte[] bytes = filename.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            if ((b >= '0' && b <= '9') || 
                (b >= 'A' && b <= 'Z') || 
                (b >= 'a' && b <= 'z') ||
                b == '-' || b == '_' || b == '.' || b == '~') {
                encoded.append((char) b);
            } else {
                encoded.append(String.format("%%%02X", b & 0xFF));
            }
        }
        return encoded.toString();
    }
}
