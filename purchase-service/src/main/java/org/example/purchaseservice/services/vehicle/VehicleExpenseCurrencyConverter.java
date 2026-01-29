package org.example.purchaseservice.services.vehicle;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.purchaseservice.exceptions.PurchaseException;
import org.example.purchaseservice.services.exchange.IExchangeRateService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpenseCurrencyConverter {

    private static final int EXCHANGE_RATE_SCALE = 6;
    private static final String EUR_CURRENCY = "EUR";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final IExchangeRateService exchangeRateService;

    public record ConversionResult(BigDecimal exchangeRate, BigDecimal convertedAmount) {}

    public ConversionResult convertCurrency(@NonNull BigDecimal amount, @NonNull String currency,
                                            BigDecimal exchangeRate, BigDecimal convertedAmount) {
        if (EUR_CURRENCY.equals(currency)) {
            return convertEurCurrency(amount, convertedAmount);
        } else {
            return convertNonEurCurrency(amount, currency, exchangeRate, convertedAmount);
        }
    }

    private ConversionResult convertEurCurrency(@NonNull BigDecimal amount, BigDecimal convertedAmount) {
        BigDecimal rate = BigDecimal.ONE;
        if (convertedAmount == null || convertedAmount.compareTo(ZERO) <= 0) {
            convertedAmount = amount;
        } else if (convertedAmount.compareTo(amount) != 0) {
            throw new PurchaseException("INVALID_EUR_CONVERSION",
                    "For EUR currency, converted amount must equal amount");
        }
        return new ConversionResult(rate, convertedAmount);
    }

    private ConversionResult convertNonEurCurrency(@NonNull BigDecimal amount, @NonNull String currency,
                                                   BigDecimal exchangeRate, BigDecimal convertedAmount) {
        BigDecimal rate = resolveExchangeRate(currency, exchangeRate);

        if (convertedAmount == null || convertedAmount.compareTo(ZERO) <= 0) {
            convertedAmount = calculateConvertedAmount(amount, rate);
        } else {
            rate = calculateExchangeRate(amount, convertedAmount);
        }

        return new ConversionResult(rate, convertedAmount);
    }

    private BigDecimal resolveExchangeRate(@NonNull String currency, BigDecimal providedRate) {
        if (providedRate != null && providedRate.compareTo(ZERO) > 0) {
            return providedRate;
        }

        try {
            BigDecimal rate = exchangeRateService.getExchangeRateToEur(currency);
            if (rate == null || rate.compareTo(ZERO) <= 0) {
                throw new PurchaseException("EXCHANGE_RATE_NOT_FOUND",
                        "Exchange rate not found for currency: " + currency);
            }
            return rate;
        } catch (PurchaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get exchange rate for currency {}: {}", currency, e.getMessage(), e);
            throw new PurchaseException("FAILED_TO_GET_EXCHANGE_RATE",
                    "Failed to get exchange rate for currency: " + currency);
        }
    }

    private BigDecimal calculateConvertedAmount(@NonNull BigDecimal amount, @NonNull BigDecimal exchangeRate) {
        if (exchangeRate.compareTo(ZERO) == 0) {
            throw new PurchaseException("INVALID_EXCHANGE_RATE",
                    "Exchange rate cannot be zero for division");
        }
        return amount.divide(exchangeRate, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateExchangeRate(@NonNull BigDecimal amount, @NonNull BigDecimal convertedAmount) {
        if (convertedAmount.compareTo(ZERO) == 0) {
            throw new PurchaseException("INVALID_CONVERTED_AMOUNT",
                    "Converted amount cannot be zero for exchange rate calculation");
        }
        return amount.divide(convertedAmount, EXCHANGE_RATE_SCALE, RoundingMode.HALF_UP);
    }
}
