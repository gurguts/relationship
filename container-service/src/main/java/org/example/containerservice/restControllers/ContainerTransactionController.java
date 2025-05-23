package org.example.containerservice.restControllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.containerservice.mappers.ContainerMapper;
import org.example.containerservice.models.dto.container.ContainerTransactionDTO;
import org.example.containerservice.services.ContainerTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/containers/transactions")
@RequiredArgsConstructor
public class ContainerTransactionController {

    private final ContainerTransactionService containerTransactionService;
    private final ContainerMapper containerMapper;

    @GetMapping
    public ResponseEntity<List<ContainerTransactionDTO>> getAllTransactions() {
        log.info("Fetching all container transactions");
        List<ContainerTransactionDTO> transactions = containerTransactionService.getAllTransactions()
                .stream()
                .map(containerMapper::toContainerTransactionDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(transactions);
    }
}