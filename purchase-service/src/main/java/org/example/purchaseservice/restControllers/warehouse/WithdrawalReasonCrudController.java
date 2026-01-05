package org.example.purchaseservice.restControllers.warehouse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WithdrawalReasonMapper;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalReasonCreateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalReasonDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalReasonUpdateDTO;
import org.example.purchaseservice.services.impl.IWithdrawalReasonService;
import org.example.purchaseservice.models.warehouse.WithdrawalReason;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/withdrawal-reason")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WithdrawalReasonCrudController {
    private final WithdrawalReasonMapper withdrawalReasonMapper;
    private final IWithdrawalReasonService withdrawalReasonService;

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/{id}")
    public ResponseEntity<WithdrawalReasonDTO> getWithdrawalReason(@PathVariable @Positive Long id) {
        WithdrawalReason withdrawalReason = withdrawalReasonService.getWithdrawalReason(id);
        WithdrawalReasonDTO withdrawalReasonDTO = withdrawalReasonMapper.withdrawalReasonToWithdrawalReasonDTO(withdrawalReason);
        return ResponseEntity.ok(withdrawalReasonDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping
    public ResponseEntity<List<WithdrawalReasonDTO>> getWithdrawalReasons() {
        List<WithdrawalReason> withdrawalReasons = withdrawalReasonService.getAllWithdrawalReasons();
        List<WithdrawalReasonDTO> dtos = withdrawalReasons.stream()
                .map(withdrawalReasonMapper::withdrawalReasonToWithdrawalReasonDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PostMapping
    public ResponseEntity<WithdrawalReasonDTO> createWithdrawalReason(@RequestBody @Valid @NonNull WithdrawalReasonCreateDTO withdrawalReasonCreateDTO) {
        WithdrawalReason withdrawalReason = withdrawalReasonMapper.withdrawalReasonCreateDTOToWithdrawalReason(withdrawalReasonCreateDTO);
        WithdrawalReason created = withdrawalReasonService.createWithdrawalReason(withdrawalReason);
        WithdrawalReasonDTO createdWithdrawalReason = withdrawalReasonMapper.withdrawalReasonToWithdrawalReasonDTO(created);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdWithdrawalReason.getId())
                .toUri();
        return ResponseEntity.created(location).body(createdWithdrawalReason);
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @PatchMapping("/{id}")
    public ResponseEntity<WithdrawalReasonDTO> updateWithdrawalReason(
            @PathVariable @Positive Long id,
            @RequestBody @Valid @NonNull WithdrawalReasonUpdateDTO withdrawalReasonUpdateDTO) {
        WithdrawalReason withdrawalReason = withdrawalReasonMapper.withdrawalReasonUpdateDTOToWithdrawalReason(withdrawalReasonUpdateDTO);
        WithdrawalReason updatedWithdrawalReason = withdrawalReasonService.updateWithdrawalReason(id, withdrawalReason);
        return ResponseEntity.ok(withdrawalReasonMapper.withdrawalReasonToWithdrawalReasonDTO(updatedWithdrawalReason));
    }

    @PreAuthorize("hasAuthority('administration:edit')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWithdrawalReason(@PathVariable @Positive Long id) {
        withdrawalReasonService.deleteWithdrawalReason(id);
        return ResponseEntity.noContent().build();
    }
}
