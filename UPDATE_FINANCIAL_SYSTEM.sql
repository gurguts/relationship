-- =====================================================
-- FINANCIAL SYSTEM DATABASE SETUP
-- =====================================================
-- Execute this SQL script in your MySQL database
-- This script creates all necessary tables for the new financial control system
--
-- IMPORTANT: Make a backup before running!
-- mysqldump -u root -p export_data > backup_before_financial_system.sql
-- =====================================================

USE export_data;

-- =====================================================
-- STEP 1: CREATE NEW TABLES
-- =====================================================

-- Table: branches
-- Stores branches (филии) for organizing accounts
CREATE TABLE IF NOT EXISTS `branches` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_branch_name` (`name`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: accounts
-- Stores accounts (счета) - can be linked to users or standalone
CREATE TABLE IF NOT EXISTS `accounts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `user_id` BIGINT DEFAULT NULL,
    `branch_id` BIGINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_branch_id` (`branch_id`),
    KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: account_currencies
-- Stores supported currencies for each account
CREATE TABLE IF NOT EXISTS `account_currencies` (
    `account_id` BIGINT NOT NULL,
    `currency` VARCHAR(3) NOT NULL,
    PRIMARY KEY (`account_id`, `currency`),
    CONSTRAINT `fk_account_currencies_account` 
        FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: account_balances
-- Stores balance for each currency in each account
CREATE TABLE IF NOT EXISTS `account_balances` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `account_id` BIGINT NOT NULL,
    `currency` VARCHAR(3) NOT NULL,
    `amount` DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_currency` (`account_id`, `currency`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_currency` (`currency`),
    CONSTRAINT `fk_account_balances_account` 
        FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: transaction_categories
-- Stores configurable transaction categories for each transaction type
CREATE TABLE IF NOT EXISTS `transaction_categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `type` VARCHAR(50) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` TEXT,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_name` (`type`, `name`),
    KEY `idx_type` (`type`),
    KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: branch_permissions
-- Stores user permissions for each branch
CREATE TABLE IF NOT EXISTS `branch_permissions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `branch_id` BIGINT NOT NULL,
    `can_view` BOOLEAN NOT NULL DEFAULT FALSE,
    `can_operate` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_branch` (`user_id`, `branch_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_branch_id` (`branch_id`),
    CONSTRAINT `fk_branch_permissions_branch` 
        FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- STEP 2: UPDATE EXISTING TRANSACTIONS TABLE
-- =====================================================

-- Add new columns to transactions table for new financial system
ALTER TABLE `transactions` 
    MODIFY COLUMN `target_user_id` BIGINT DEFAULT NULL COMMENT 'Опционально, для обратной совместимости',
    MODIFY COLUMN `type` VARCHAR(50) NOT NULL COMMENT 'Тип транзакции (расширен для новых типов)',
    ADD COLUMN `from_account_id` BIGINT DEFAULT NULL COMMENT 'Счет-источник' AFTER `target_user_id`,
    ADD COLUMN `to_account_id` BIGINT DEFAULT NULL COMMENT 'Счет-получатель' AFTER `from_account_id`,
    ADD COLUMN `category_id` BIGINT DEFAULT NULL COMMENT 'Категория транзакции' AFTER `type`,
    ADD COLUMN `exchange_rate` DECIMAL(20,6) DEFAULT NULL COMMENT 'Курс обмена для конвертации' AFTER `currency`,
    ADD COLUMN `converted_currency` VARCHAR(3) DEFAULT NULL COMMENT 'Валюта конвертации' AFTER `exchange_rate`,
    ADD COLUMN `converted_amount` DECIMAL(20,2) DEFAULT NULL COMMENT 'Конвертированная сумма' AFTER `converted_currency`,
    MODIFY COLUMN `amount` DECIMAL(20,2) NOT NULL,
    MODIFY COLUMN `currency` VARCHAR(3) DEFAULT NULL,
    MODIFY COLUMN `description` TEXT;

-- Add indexes for new columns
ALTER TABLE `transactions`
    ADD KEY `idx_from_account_id` (`from_account_id`),
    ADD KEY `idx_to_account_id` (`to_account_id`),
    ADD KEY `idx_category_id` (`category_id`);

-- =====================================================
-- STEP 3: INSERT DEFAULT CATEGORIES (OPTIONAL)
-- =====================================================

-- Insert some default categories for each transaction type
-- These can be modified/deleted later through the admin interface

-- Categories for INTERNAL_TRANSFER
INSERT IGNORE INTO `transaction_categories` (`type`, `name`, `description`, `is_active`) VALUES
('INTERNAL_TRANSFER', 'Перевод на ФОП', 'Перевод средств на ФОП', TRUE),
('INTERNAL_TRANSFER', 'Бензин', 'Оплата бензина', TRUE),
('INTERNAL_TRANSFER', 'Зарплата', 'Выплата зарплаты', TRUE);

-- Categories for EXTERNAL_INCOME
INSERT IGNORE INTO `transaction_categories` (`type`, `name`, `description`, `is_active`) VALUES
('EXTERNAL_INCOME', 'Оплата от заказчика', 'Поступление средств от заказчика', TRUE),
('EXTERNAL_INCOME', 'Возврат средств', 'Возврат ранее уплаченных средств', TRUE);

-- Categories for EXTERNAL_EXPENSE
INSERT IGNORE INTO `transaction_categories` (`type`, `name`, `description`, `is_active`) VALUES
('EXTERNAL_EXPENSE', 'Оплата поставщику', 'Оплата поставщику товаров/услуг', TRUE),
('EXTERNAL_EXPENSE', 'Налоги', 'Уплата налогов', TRUE),
('EXTERNAL_EXPENSE', 'Коммунальные услуги', 'Оплата коммунальных услуг', TRUE);

-- Categories for CLIENT_PAYMENT
INSERT IGNORE INTO `transaction_categories` (`type`, `name`, `description`, `is_active`) VALUES
('CLIENT_PAYMENT', 'Аванс клиенту', 'Выплата аванса клиенту', TRUE),
('CLIENT_PAYMENT', 'Окончательный расчет', 'Окончательный расчет с клиентом', TRUE);

-- =====================================================
-- STEP 4: MIGRATION NOTES
-- =====================================================

-- NOTE: Existing transactions will continue to work with target_user_id
-- New transactions should use from_account_id / to_account_id
-- 
-- To migrate existing user balances to new account system:
-- 1. Create accounts for each user (or create one account per user)
-- 2. Create account_balances for UAH, EUR, USD based on existing balances table
-- 3. Link accounts to users via user_id
--
-- Example migration script (run separately after creating accounts):
-- INSERT INTO account_balances (account_id, currency, amount)
-- SELECT a.id, 'UAH', b.balance_uah FROM accounts a
-- JOIN balances b ON a.user_id = b.user_id
-- WHERE a.user_id IS NOT NULL;
--
-- Similar for EUR and USD currencies

