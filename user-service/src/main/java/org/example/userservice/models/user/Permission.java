package org.example.userservice.models.user;

import lombok.Getter;

@Getter
public enum Permission {
    SYSTEM_ADMIN("system:admin"),

    USER_CREATE("user:create"),
    USER_EDIT("user:edit"),
    USER_DELETE("user:delete"),

    CLIENT_VIEW("client:view"),
    CLIENT_CREATE("client:create"),
    CLIENT_EDIT("client:edit"),
    CLIENT_EDIT_STRANGERS("client:edit_strangers"),
    CLIENT_STRANGER_EDIT("client_stranger:edit"),
    CLIENT_DELETE("client:delete"),
    CLIENT_FULL_DELETE("client:full_delete"),
    CLIENT_EDIT_SOURCE("client:edit_source"),
    CLIENT_EXCEL("client:excel"),

    SALE_VIEW("sale:view"),
    SALE_CREATE("sale:create"),
    SALE_EDIT("sale:edit"),
    SALE_EDIT_STRANGERS("sale:edit_strangers"),
    SALE_DELETE("sale:delete"),
    SALE_EDIT_SOURCE("sale:edit_source"),
    SALE_EXCEL("sale:excel"),

    PURCHASE_VIEW("purchase:view"),
    PURCHASE_CREATE("purchase:create"),
    PURCHASE_EDIT("purchase:edit"),
    PURCHASE_EDIT_STRANGERS("purchase:edit_strangers"),
    PURCHASE_DELETE("purchase:delete"),
    PURCHASE_EDIT_SOURCE("purchase:edit_source"),
    PURCHASE_EXCEL("purchase:excel"),

    CONTAINER_VIEW("container:view"),
    CONTAINER_TRANSFER("container:transfer"),
    CONTAINER_BALANCE("container:balance"),
    CONTAINER_EXCEL("container:excel"),

    FINANCE_VIEW("finance:view"),
    FINANCE_BALANCE_EDIT("finance:balance_edit"),
    FINANCE_TRANSFER_VIEW("finance:transfer_view"),
    FINANCE_TRANSFER_EXCEL("finance:transfer_excel"),
    FINANCE_TRANSACTION_DELETE("transaction:delete"),

    WAREHOUSE_VIEW("warehouse:view"),
    WAREHOUSE_CREATE("warehouse:create"),
    WAREHOUSE_EDIT("warehouse:edit"),
    WAREHOUSE_DELETE("warehouse:delete"),
    WAREHOUSE_WITHDRAW("warehouse:withdraw"),
    WAREHOUSE_EXCEL("warehouse:excel"),

    INVENTORY_VIEW("inventory:view"),
    INVENTORY_MANAGE("inventory:manage"),

    ANALYTICS_VIEW("analytics:view"),

    SETTINGS_VIEW("settings:view"),
    SETTINGS_CLIENT_CREATE("settings_client:create"),
    SETTINGS_CLIENT_EDIT("settings_client:edit"),
    SETTINGS_CLIENT_DELETE("settings_client:delete"),
    SETTINGS_FINANCE_CREATE("settings_finance:create"),
    SETTINGS_FINANCE_EDIT("settings_finance:edit"),
    SETTINGS_EXCHANGE_EDIT("settings_exchange:edit"),
    SETTINGS_FINANCE_DELETE("settings_finance:delete"),

    SETTINGS_EDIT("settings:edit"),

    ADMINISTRATION_VIEW("administration:view"),
    ADMINISTRATION_EDIT("administration:edit");

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

}