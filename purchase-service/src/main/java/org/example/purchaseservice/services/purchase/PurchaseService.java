package org.example.purchaseservice.services.purchase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.models.Purchase;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    public void calculateAndSetUnitPrice(Purchase purchase) {
        if (purchase == null) {
            log.warn("Cannot calculate unit price: purchase is null");
            return;
        }

        BigDecimal quantity = purchase.getQuantity();
        BigDecimal totalPrice = purchase.getTotalPrice();

        if (quantity != null && totalPrice != null && quantity.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal unitPrice = totalPrice.divide(quantity, 6, RoundingMode.CEILING);
            purchase.setUnitPrice(unitPrice);
            log.debug("Calculated unit price: {} for purchase id={}", unitPrice, purchase.getId());
        } else {
            purchase.setUnitPrice(BigDecimal.ZERO);
            log.debug("Set unit price to ZERO for purchase id={}", purchase.getId());
        }
    }

    public void calculateAndSetPricesInEur(Purchase purchase, BigDecimal exchangeRateToEur) {
        if (purchase == null) {
            log.warn("Cannot calculate prices in EUR: purchase is null");
            return;
        }

        BigDecimal totalPrice = purchase.getTotalPrice();
        BigDecimal quantity = purchase.getQuantity();

        if (totalPrice == null || quantity == null) {
            log.debug("Cannot calculate prices in EUR: totalPrice or quantity is null for purchase id={}", purchase.getId());
            return;
        }

        String currency = purchase.getCurrency();
        if ("EUR".equalsIgnoreCase(currency) || currency == null) {
            purchase.setTotalPriceEur(totalPrice);
            purchase.setUnitPriceEur(purchase.getUnitPrice());
            log.debug("Purchase currency is EUR, set prices directly for purchase id={}", purchase.getId());
        } else {
            if (exchangeRateToEur != null && exchangeRateToEur.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal totalPriceEur = totalPrice.divide(exchangeRateToEur, 6, RoundingMode.HALF_UP);
                purchase.setTotalPriceEur(totalPriceEur);

                if (quantity.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal unitPriceEur = totalPriceEur.divide(quantity, 6, RoundingMode.CEILING);
                    purchase.setUnitPriceEur(unitPriceEur);
                } else {
                    purchase.setUnitPriceEur(BigDecimal.ZERO);
                }
                log.debug("Calculated prices in EUR: totalPriceEur={}, unitPriceEur={} for purchase id={}", 
                        totalPriceEur, purchase.getUnitPriceEur(), purchase.getId());
            } else {
                purchase.setTotalPriceEur(totalPrice);
                purchase.setUnitPriceEur(purchase.getUnitPrice());
                log.debug("Exchange rate to EUR is invalid, set prices directly for purchase id={}", purchase.getId());
            }
        }
    }
}

