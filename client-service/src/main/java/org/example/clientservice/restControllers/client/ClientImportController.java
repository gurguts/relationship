package org.example.clientservice.restControllers.client;

import lombok.RequiredArgsConstructor;
import org.example.clientservice.services.impl.IClientImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/client/import")
@RequiredArgsConstructor
public class ClientImportController {
    private final IClientImportService clientImportService;
    
    @PreAuthorize("hasAuthority('client:create')")
    @GetMapping("/template/{clientTypeId}")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long clientTypeId) {
        byte[] template = clientImportService.generateTemplate(clientTypeId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "client_import_template.xlsx");
        headers.setContentLength(template.length);
        
        return new ResponseEntity<>(template, headers, HttpStatus.OK);
    }
    
    @PreAuthorize("hasAuthority('client:create')")
    @PostMapping("/{clientTypeId}")
    public ResponseEntity<String> importClients(
            @PathVariable Long clientTypeId,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файл не може бути порожнім");
        }
        
        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            return ResponseEntity.badRequest().body("Підтримуються тільки файли Excel (.xlsx, .xls)");
        }
        
        String result = clientImportService.importClients(clientTypeId, file);
        return ResponseEntity.ok(result);
    }
}

