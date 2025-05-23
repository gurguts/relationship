package org.example.saleservice.services;

import lombok.RequiredArgsConstructor;
import org.example.saleservice.clients.ClientApiClient;
import org.example.saleservice.clients.SourceClient;
import org.example.saleservice.clients.TransactionApiClient;
import org.example.saleservice.clients.UserClient;
import org.example.saleservice.exceptions.SaleException;
import org.example.saleservice.exceptions.SaleNotFoundException;
import org.example.saleservice.models.PaymentMethod;
import org.example.saleservice.models.Sale;
import org.example.saleservice.models.dto.transaction.TransactionSaleCreateDTO;
import org.example.saleservice.repositories.SaleRepository;
import org.example.saleservice.services.impl.ISaleCrudService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SaleCrudService implements ISaleCrudService {
    private final SaleRepository saleRepository;
    private final ClientApiClient clientApiClient;
    private final TransactionApiClient transactionApiClient;
    private final SourceClient sourceClient;
    private final UserClient userCLient;

    @Override
    @Transactional
    public Sale createSale(Sale sale) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getDetails();
        sale.setUser(userId);

        sale.calculateAndSetUnitPrice();

        Long transactionId;
        if (sale.getPaymentMethod().equals(PaymentMethod.CASH)) {
            transactionId = transactionApiClient.createTransactionSale(
                    new TransactionSaleCreateDTO(userId, userId, sale.getClient(), sale.getTotalPrice(),
                            sale.getProduct(), sale.getCurrency()));
        } else {
            transactionId = transactionApiClient.createTransactionSale(
                    new TransactionSaleCreateDTO(11L, userId, sale.getClient(), sale.getTotalPrice(),
                            sale.getProduct(), sale.getCurrency()));
        }
        sale.setTransaction(transactionId);

        clientApiClient.setUrgentlyFalseAndRoute(sale.getClient());

        return saleRepository.save(sale);
    }

    @Override
    @Transactional
    public Sale updateSale(Long id, Sale updatedSale) {
        Sale existingSale = findSaleById(id);

        String fullName = getFullName();

        String sourceName = sourceClient.getSourceName(existingSale.getSource()).getName();

        boolean canEditData = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> "purchase:edit_strangers".equals(auth.getAuthority()));


        if (!(fullName.equals(sourceName) || !canEditData)) {
            throw new SaleException("ONLY_OWNER", "You cannot change someone else's purchase.");
        }

        if (updatedSale.getProduct() != null && !Objects.equals(updatedSale.getProduct(), existingSale.getProduct())) {
            existingSale.setProduct(updatedSale.getProduct());
        }

        if (updatedSale.getQuantity() != null &&
                (existingSale.getQuantity() == null ||
                        updatedSale.getQuantity().compareTo(existingSale.getQuantity()) != 0)) {
            existingSale.setQuantity(updatedSale.getQuantity());
            existingSale.calculateAndSetUnitPrice();
        }

        if (updatedSale.getTotalPrice() != null &&
                (existingSale.getTotalPrice() == null ||
                        updatedSale.getTotalPrice().compareTo(existingSale.getTotalPrice()) != 0)) {
            existingSale.setTotalPrice(updatedSale.getTotalPrice());
            existingSale.calculateAndSetUnitPrice();

            if (existingSale.getTransaction() != null) {
                transactionApiClient.updateTransactionAmount(existingSale.getTransaction(), updatedSale.getTotalPrice());
            }
        }

        if (updatedSale.getCreatedAt() != null &&
                (existingSale.getCreatedAt() == null ||
                        !updatedSale.getCreatedAt().toLocalDate().equals(existingSale.getCreatedAt().toLocalDate()))) {
            existingSale.setCreatedAt(updatedSale.getCreatedAt());
        }

        if (updatedSale.getSource() != null && !Objects.equals(updatedSale.getSource(), existingSale.getSource())) {
            boolean canEditSource = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(auth -> "sale:edit_source".equals(auth.getAuthority()));

            if (!canEditSource) {
                throw new SaleException("ONLY_ADMIN", "Only users with ADMIN role can update sourceId");
            }
            existingSale.setSource(updatedSale.getSource());
        }

        return saleRepository.save(existingSale);
    }

    private String getFullName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String login = authentication.getName();

        return userCLient.getUserFullNameFromLogin(login);
    }

    @Override
    public Sale findSaleById(Long id) {
        if (id == null) {
            throw new SaleException("ID cannot be null");
        }
        return saleRepository.findById(id)
                .orElseThrow(() -> new SaleNotFoundException(String.format("Sale with ID %d not found", id)));
    }

    @Override
    @Transactional
    public void deleteSale(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new SaleNotFoundException(String.format("Sale not found with id: %d", id)));
        transactionApiClient.deleteTransaction(sale.getTransaction());
        saleRepository.deleteById(id);
    }
}
