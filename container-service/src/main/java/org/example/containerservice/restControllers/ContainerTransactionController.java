package org.example.containerservice.restControllers;

import lombok.RequiredArgsConstructor;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.dto.container.ContainerTransactionDTO;
import org.example.containerservice.services.ContainerTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/containers/transactions")
@RequiredArgsConstructor
@Validated
public class ContainerTransactionController {

    private final ContainerTransactionService containerTransactionService;
    private final ContainerMapper containerMapper;

    @GetMapping
    public ResponseEntity<List<ContainerTransactionDTO>> getAllTransactions() {
        List<ContainerTransactionDTO> transactions = containerTransactionService.getAllTransactions()
                .stream()
                .map(containerMapper::toContainerTransactionDTO)
                .toList();
        return ResponseEntity.ok(transactions);
    }
}