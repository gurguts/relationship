package org.example.clientservice.services.client;

import feign.FeignException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.clientservice.clients.ContainerClient;
import org.example.clientservice.clients.PurchaseClient;
import org.example.clientservice.exceptions.client.ClientException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientDeletionValidator {

    private final PurchaseClient purchaseClient;
    private final ContainerClient containerClient;

    public void checkRelatedEntities(@NonNull Long clientId) {
        checkPurchases(clientId);
        checkContainers(clientId);
    }

    private void checkPurchases(@NonNull Long clientId) {
        try {
            ResponseEntity<List<Map<String, Object>>> purchasesResponse = purchaseClient.getPurchasesByClientId(clientId);
            if (purchasesResponse.getBody() != null && !purchasesResponse.getBody().isEmpty()) {
                throw new ClientException("DELETE_FORBIDDEN",
                        "Cannot delete client because there are purchases associated with it");
            }
        } catch (ClientException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Failed to check purchases for client {}: status={}, message={}", clientId, e.status(), e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while checking purchases for client {}: {}", clientId, e.getMessage(), e);
        }
    }

    private void checkContainers(@NonNull Long clientId) {
        try {
            ResponseEntity<List<Map<String, Object>>> containersResponse = containerClient.getClientContainers(clientId);
            if (containersResponse.getBody() != null && !containersResponse.getBody().isEmpty()) {
                throw new ClientException("DELETE_FORBIDDEN",
                        "Cannot delete client because there are containers associated with it");
            }
        } catch (ClientException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Failed to check containers for client {}: status={}, message={}", clientId, e.status(), e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while checking containers for client {}: {}", clientId, e.getMessage(), e);
        }
    }
}
