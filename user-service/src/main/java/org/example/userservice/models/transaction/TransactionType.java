package org.example.userservice.models.transaction;

public enum TransactionType {
    WITHDRAWAL,
    DEPOSIT,
    INTERNAL_TRANSFER,
    EXTERNAL_INCOME,
    EXTERNAL_EXPENSE,
    CLIENT_PAYMENT,
    CURRENCY_CONVERSION,
    PURCHASE
}