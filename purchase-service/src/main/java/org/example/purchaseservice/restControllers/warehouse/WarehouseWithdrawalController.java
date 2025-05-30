package org.example.purchaseservice.restControllers.warehouse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.mappers.WarehouseWithdrawalMapper;
import org.example.purchaseservice.models.PageResponse;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalDTO;
import org.example.purchaseservice.models.warehouse.WarehouseWithdrawal;
import org.example.purchaseservice.models.dto.warehouse.WarehouseWithdrawalUpdateDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalDTO;
import org.example.purchaseservice.models.dto.warehouse.WithdrawalCreateDTO;
import org.example.purchaseservice.services.impl.IWarehouseWithdrawService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseWithdrawalController {
    private final IWarehouseWithdrawService warehouseWithdrawService;
    private final WarehouseWithdrawalMapper warehouseWithdrawalMapper;

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PostMapping("/withdraw")
    public ResponseEntity<WarehouseWithdrawalDTO> createWithdrawal(@RequestBody WithdrawalCreateDTO request) {
        WarehouseWithdrawal warehouseWithdrawal =
                warehouseWithdrawService.createWithdrawal(
                        warehouseWithdrawalMapper.withdrawalCreateDTOToWarehouseWithdrawal(request));

        WarehouseWithdrawalDTO warehouseWithdrawalDTO =
                warehouseWithdrawalMapper.warehouseWithdrawalToWarehouseWithdrawalDTO(warehouseWithdrawal);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(warehouseWithdrawalDTO.getId())
                .toUri();
        return ResponseEntity.created(location).body(warehouseWithdrawalDTO);
    }

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @PutMapping("/withdraw/{id}")
    public ResponseEntity<WarehouseWithdrawalDTO> updateWithdrawal(
            @PathVariable Long id,
            @RequestBody WarehouseWithdrawalUpdateDTO warehouseWithdrawalUpdateDTO) {

        WarehouseWithdrawal request =
                warehouseWithdrawalMapper.withdrawalUpdateDTOToWarehouseWithdrawal(warehouseWithdrawalUpdateDTO);

        WarehouseWithdrawal updated = warehouseWithdrawService.updateWithdrawal(id, request);

        return ResponseEntity.ok(warehouseWithdrawalMapper.warehouseWithdrawalToWarehouseWithdrawalDTO(updated));
    }

    @PreAuthorize("hasAuthority('warehouse:withdraw')")
    @DeleteMapping("/withdraw/{id}")
    public ResponseEntity<Void> deleteWithdrawal(@PathVariable Long id) {
        warehouseWithdrawService.deleteWithdrawal(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('warehouse:view')")
    @GetMapping("/withdrawals")
    public ResponseEntity<PageResponse<WithdrawalDTO>> getWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "withdrawalDate") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String filters) throws JsonProcessingException {
        Map<String, List<String>> filterMap = filters != null && !filters.isEmpty()
                ? new ObjectMapper().readValue(filters, new TypeReference<>() {
        })
                : Collections.emptyMap();

        PageResponse<WithdrawalDTO> result =
                warehouseWithdrawService.getWithdrawals(page, size, sort, direction, filterMap);

        return ResponseEntity.ok(result);
    }
}
