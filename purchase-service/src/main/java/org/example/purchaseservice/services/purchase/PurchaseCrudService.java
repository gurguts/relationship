package org.example.purchaseservice.services.purchase;

import lombok.RequiredArgsConstructor;
import org.example.purchaseservice.clients.ClientApiClient;
import org.example.purchaseservice.clients.SourceClient;
import org.example.purchaseservice.clients.TransactionApiClient;
import org.example.purchaseservice.clients.UserClient;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.exceptions.PurchaseNotFoundException;
import org.example.purchaseservice.models.PaymentMethod;
import org.example.purchaseservice.models.Purchase;
import org.example.purchaseservice.models.dto.purchase.TransactionPurchaseCreateDTO;
import org.example.purchaseservice.repositories.PurchaseRepository;
import org.example.purchaseservice.services.impl.IPurchaseCrudService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PurchaseCrudService implements IPurchaseCrudService {
    private final PurchaseRepository purchaseRepository;
    private final ClientApiClient clientApiClient;
    private final TransactionApiClient transactionApiClient;
    private final SourceClient sourceClient;
    private final UserClient userCLient;

    @Override
    @Transactional
    public Purchase createPurchase(Purchase purchase) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        purchase.setUser(userId);

        purchase.calculateAndSetUnitPrice();

        Long transactionId;
        if (purchase.getPaymentMethod().equals(PaymentMethod.CASH)) {
            transactionId = transactionApiClient.createTransactionPurchase(
                    new TransactionPurchaseCreateDTO(userId, userId, purchase.getClient(), purchase.getTotalPrice(),
                            purchase.getProduct(), purchase.getCurrency()));
        } else {
            transactionId = transactionApiClient.createTransactionPurchase(
                    new TransactionPurchaseCreateDTO(11L, userId, purchase.getClient(),
                            purchase.getTotalPrice(), purchase.getProduct(), purchase.getCurrency()));
        }
        purchase.setTransaction(transactionId);

        clientApiClient.setUrgentlyFalseAndRoute(purchase.getClient());

        return purchaseRepository.save(purchase);
    }

    @Override
    @Transactional
    public Purchase updatePurchase(Long id, Purchase updatedPurchase) {
        Purchase existingPurchase = findPurchaseById(id);

        String fullName = getFullName();

        String sourceName = sourceClient.getSourceName(existingPurchase.getSource()).getName();

        boolean canEditData = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> "purchase:edit_strangers".equals(auth.getAuthority()));

        if (!(fullName.equals(sourceName) || canEditData)) {
            throw new PurchaseException("ONLY_OWNER", "You cannot change someone else's purchase.");
        }

        if (updatedPurchase.getProduct() != null && !Objects.equals(updatedPurchase.getProduct(),
                existingPurchase.getProduct())) {
            existingPurchase.setProduct(updatedPurchase.getProduct());
        }

        if (updatedPurchase.getQuantity() != null &&
                (existingPurchase.getQuantity() == null || updatedPurchase.getQuantity().compareTo(
                        existingPurchase.getQuantity()) != 0)) {
            existingPurchase.setQuantity(updatedPurchase.getQuantity());
            existingPurchase.calculateAndSetUnitPrice();
        }

        if (updatedPurchase.getTotalPrice() != null &&
                (existingPurchase.getTotalPrice() == null || updatedPurchase.getTotalPrice().compareTo(
                        existingPurchase.getTotalPrice()) != 0)) {
            existingPurchase.setTotalPrice(updatedPurchase.getTotalPrice());
            existingPurchase.calculateAndSetUnitPrice();

            if (existingPurchase.getTransaction() != null) {
                transactionApiClient.updateTransactionAmount(
                        existingPurchase.getTransaction(), updatedPurchase.getTotalPrice());
            }
        }

        if (updatedPurchase.getCreatedAt() != null &&
                (existingPurchase.getCreatedAt() == null ||
                        !updatedPurchase.getCreatedAt().toLocalDate().equals(
                                existingPurchase.getCreatedAt().toLocalDate()))) {
            existingPurchase.setCreatedAt(updatedPurchase.getCreatedAt());
        }

        if (updatedPurchase.getSource() != null && !Objects.equals(updatedPurchase.getSource(),
                existingPurchase.getSource())) {
            boolean canEditSource = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(auth -> "purchase:edit_source".equals(auth.getAuthority()));

            if (!canEditSource) {
                throw new PurchaseException("ONLY_ADMIN", "Only users with ADMIN role can update sourceId");
            }
            existingPurchase.setSource(updatedPurchase.getSource());
        }

        return purchaseRepository.save(existingPurchase);
    }

    private String getFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();

        return userCLient.getUserFullNameFromLogin(login);
    }

    @Override
    public Purchase findPurchaseById(Long id) {
        if (id == null) {
            throw new PurchaseException("ID cannot be null");
        }
        return purchaseRepository.findById(id)
                .orElseThrow(() -> new PurchaseNotFoundException(String.format("Purchase with ID %d not found", id)));
    }

    @Override
    @Transactional
    public void deletePurchase(Long id) {
        Long transactionId = purchaseRepository.findById(id).orElseThrow(() ->
                new PurchaseNotFoundException(String.format("Purchase with ID %d not found", id))).getTransaction();
        transactionApiClient.deleteTransaction(transactionId);
        purchaseRepository.deleteById(id);
    }
}
