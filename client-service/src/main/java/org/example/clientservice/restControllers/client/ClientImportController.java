package org.example.clientservice.restControllers.client;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.clientservice.services.impl.IClientImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/client/import")
@RequiredArgsConstructor
@Validated
public class ClientImportController {
    private final IClientImportService clientImportService;
    
    @PreAuthorize("hasAuthority('client:create')")
    @GetMapping("/template/{clientTypeId}")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable @Positive Long clientTypeId) {
        byte[] template = clientImportService.generateTemplate(clientTypeId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "client_import_template.xlsx");
        headers.setContentLength(template.length);
        
        return ResponseEntity.ok().headers(headers).body(template);
    }
    
    @PreAuthorize("hasAuthority('client:create')")
    @PostMapping("/{clientTypeId}")
    public ResponseEntity<String> importClients(
            @PathVariable @Positive Long clientTypeId,
            @RequestParam("file") @NotNull MultipartFile file) {
        String result = clientImportService.importClients(clientTypeId, file);
        return ResponseEntity.ok(result);
    }
}

