-- =====================================================
-- BALANCE SYSTEM DATABASE SETUP
-- =====================================================
-- Execute this SQL script in your MySQL database BEFORE starting the application
-- This script creates all necessary tables and columns for the balance tracking system
--
-- IMPORTANT: Make a backup before running!
-- mysqldump -u root -p export_data > backup_before_balance_system.sql
-- =====================================================

USE export_data;

-- =====================================================
-- STEP 1: CREATE NEW TABLES
-- =====================================================

-- Table: driver_product_balances
-- Stores product balance for each driver with average price
CREATE TABLE IF NOT EXISTS `driver_product_balances` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `driver_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    `average_price_uah` DECIMAL(20,6) NOT NULL DEFAULT 0.00,
    `total_cost_uah` DECIMAL(20,6) NOT NULL DEFAULT 0.00,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_driver_product` (`driver_id`, `product_id`),
    KEY `idx_driver_id` (`driver_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_quantity` (`quantity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: warehouse_product_balances
-- Stores product balance for each warehouse with average price
CREATE TABLE IF NOT EXISTS `warehouse_product_balances` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `warehouse_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` DECIMAL(20,2) NOT NULL DEFAULT 0.00,
    `average_price_uah` DECIMAL(20,6) NOT NULL DEFAULT 0.00,
    `total_cost_uah` DECIMAL(20,6) NOT NULL DEFAULT 0.00,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_warehouse_product` (`warehouse_id`, `product_id`),
    KEY `idx_warehouse_id` (`warehouse_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_quantity` (`quantity`),
    CONSTRAINT `fk_warehouse_product_balance_warehouse` 
        FOREIGN KEY (`warehouse_id`) REFERENCES `warehouses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: shipments
-- Groups product withdrawals by vehicle to calculate total cost
CREATE TABLE IF NOT EXISTS `shipments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `shipment_date` DATE NOT NULL,
    `vehicle_number` VARCHAR(50) DEFAULT NULL,
    `invoice_ua` VARCHAR(100) DEFAULT NULL,
    `invoice_eu` VARCHAR(100) DEFAULT NULL,
    `description` TEXT,
    `total_cost_uah` DECIMAL(20,6) NOT NULL DEFAULT 0.00,
    `user_id` BIGINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shipment_date` (`shipment_date`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: shipment_products
-- Tracks products added to shipments (vehicles)
CREATE TABLE IF NOT EXISTS `shipment_products` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `shipment_id` BIGINT NOT NULL,
    `warehouse_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` DECIMAL(20,2) NOT NULL,
    `unit_price_uah` DECIMAL(20,6) NOT NULL,
    `total_cost_uah` DECIMAL(20,6) NOT NULL,
    `added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `user_id` BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_shipment_id` (`shipment_id`),
    KEY `idx_warehouse_id` (`warehouse_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_added_at` (`added_at`),
    CONSTRAINT `fk_shipment_products_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: warehouse_discrepancies
-- Tracks losses and gains when warehouse clerk receives goods from driver
CREATE TABLE IF NOT EXISTS `warehouse_discrepancies` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `warehouse_receipt_id` BIGINT NOT NULL,
    `driver_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `warehouse_id` BIGINT NOT NULL,
    `receipt_date` DATE NOT NULL,
    `purchased_quantity` DECIMAL(20,2) NOT NULL,
    `received_quantity` DECIMAL(20,2) NOT NULL,
    `discrepancy_quantity` DECIMAL(20,2) NOT NULL,
    `unit_price_uah` DECIMAL(20,6) NOT NULL,
    `discrepancy_value_uah` DECIMAL(20,6) NOT NULL,
    `type` ENUM('LOSS', 'GAIN') NOT NULL,
    `comment` TEXT,
    `created_by_user_id` BIGINT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_warehouse_receipt_id` (`warehouse_receipt_id`),
    KEY `idx_driver_id` (`driver_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_warehouse_id` (`warehouse_id`),
    KEY `idx_receipt_date` (`receipt_date`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: product_transfers
-- Records product movements between different products within a warehouse
CREATE TABLE IF NOT EXISTS `product_transfers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `warehouse_id` BIGINT NOT NULL,
    `from_product_id` BIGINT NOT NULL,
    `to_product_id` BIGINT NOT NULL,
    `quantity` DECIMAL(20,2) NOT NULL,
    `unit_price_uah` DECIMAL(20,6) NOT NULL,
    `total_cost_uah` DECIMAL(20,6) NOT NULL,
    `transfer_date` DATE NOT NULL,
    `user_id` BIGINT NOT NULL,
    `reason_id` BIGINT NOT NULL,
    `description` TEXT,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_warehouse_id` (`warehouse_id`),
    KEY `idx_from_product_id` (`from_product_id`),
    KEY `idx_to_product_id` (`to_product_id`),
    KEY `idx_transfer_date` (`transfer_date`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_reason_id` (`reason_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table: warehouse_balance_adjustments
-- Records balance adjustments for a warehouse's product inventory
CREATE TABLE IF NOT EXISTS `warehouse_balance_adjustments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `warehouse_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `previous_quantity` DECIMAL(20,2) NOT NULL,
    `new_quantity` DECIMAL(20,2) NOT NULL,
    `previous_total_cost` DECIMAL(20,6) NOT NULL,
    `new_total_cost` DECIMAL(20,6) NOT NULL,
    `previous_average_price` DECIMAL(20,6) NOT NULL,
    `new_average_price` DECIMAL(20,6) NOT NULL,
    `adjustment_type` VARCHAR(20) NOT NULL,
    `description` TEXT,
    `user_id` BIGINT,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_wba_warehouse_product` (`warehouse_id`, `product_id`),
    KEY `idx_wba_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- STEP 2: ADD NEW COLUMNS TO EXISTING TABLES
-- =====================================================

-- Disable strict mode temporarily to allow duplicate key/column warnings
SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;

-- Add UAH price columns to purchases table
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='purchases' AND COLUMN_NAME='total_price_uah') > 0,
    'SELECT ''Column total_price_uah already exists'' AS Info',
    'ALTER TABLE `purchases` ADD COLUMN `total_price_uah` DECIMAL(20,6) DEFAULT NULL AFTER `total_price`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='purchases' AND COLUMN_NAME='unit_price_uah') > 0,
    'SELECT ''Column unit_price_uah already exists'' AS Info',
    'ALTER TABLE `purchases` ADD COLUMN `unit_price_uah` DECIMAL(20,6) DEFAULT NULL AFTER `unit_price`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add indexes for purchases
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='purchases' AND INDEX_NAME='idx_purchases_user_product') > 0,
    'SELECT ''Index idx_purchases_user_product already exists'' AS Info',
    'ALTER TABLE `purchases` ADD KEY `idx_purchases_user_product` (`user_id`, `product_id`)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='purchases' AND INDEX_NAME='idx_purchases_total_price_uah') > 0,
    'SELECT ''Index idx_purchases_total_price_uah already exists'' AS Info',
    'ALTER TABLE `purchases` ADD KEY `idx_purchases_total_price_uah` (`total_price_uah`)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Update existing purchases: if currency = UAH, copy prices
UPDATE `purchases`
SET 
    `total_price_uah` = `total_price`,
    `unit_price_uah` = `unit_price`
WHERE (`currency` = 'UAH' OR `currency` IS NULL)
  AND `total_price_uah` IS NULL;

-- Update existing purchases: if currency != UAH, convert via exchange_rate
UPDATE `purchases`
SET 
    `total_price_uah` = `total_price` * `exchange_rate`,
    `unit_price_uah` = `unit_price` * `exchange_rate`
WHERE `currency` != 'UAH' 
  AND `exchange_rate` IS NOT NULL 
  AND `exchange_rate` > 0
  AND `total_price_uah` IS NULL;

-- Add cost tracking columns to warehouse_entries (OLD table name, kept for migration)
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_entries' AND COLUMN_NAME='unit_price_uah') > 0,
    'SELECT ''Column unit_price_uah already exists'' AS Info',
    'ALTER TABLE `warehouse_entries` ADD COLUMN `unit_price_uah` DECIMAL(20,6) DEFAULT NULL AFTER `quantity`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_entries' AND COLUMN_NAME='total_cost_uah') > 0,
    'SELECT ''Column total_cost_uah already exists'' AS Info',
    'ALTER TABLE `warehouse_entries` ADD COLUMN `total_cost_uah` DECIMAL(20,6) DEFAULT NULL AFTER `unit_price_uah`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add driver_balance_quantity column to warehouse_receipts
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_receipts' AND COLUMN_NAME='driver_balance_quantity') > 0,
    'SELECT ''Column driver_balance_quantity already exists'' AS Info',
    'ALTER TABLE `warehouse_receipts` ADD COLUMN `driver_balance_quantity` DECIMAL(20,2) DEFAULT NULL AFTER `quantity`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add indexes for warehouse_entries
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND (TABLE_NAME='warehouse_entries' OR TABLE_NAME='warehouse_receipts') 
     AND INDEX_NAME='idx_warehouse_entries_user_product') > 0,
    'SELECT ''Index idx_warehouse_entries_user_product already exists'' AS Info',
    'ALTER TABLE `warehouse_entries` ADD KEY `idx_warehouse_entries_user_product` (`user_id`, `product_id`)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND (TABLE_NAME='warehouse_entries' OR TABLE_NAME='warehouse_receipts') 
     AND INDEX_NAME='idx_warehouse_entries_warehouse_product') > 0,
    'SELECT ''Index idx_warehouse_entries_warehouse_product already exists'' AS Info',
    'ALTER TABLE `warehouse_entries` ADD KEY `idx_warehouse_entries_warehouse_product` (`warehouse_id`, `product_id`)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add cost tracking columns to warehouse_withdrawals
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_withdrawals' AND COLUMN_NAME='unit_price_uah') > 0,
    'SELECT ''Column unit_price_uah already exists'' AS Info',
    'ALTER TABLE `warehouse_withdrawals` ADD COLUMN `unit_price_uah` DECIMAL(20,6) DEFAULT NULL AFTER `quantity`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_withdrawals' AND COLUMN_NAME='total_cost_uah') > 0,
    'SELECT ''Column total_cost_uah already exists'' AS Info',
    'ALTER TABLE `warehouse_withdrawals` ADD COLUMN `total_cost_uah` DECIMAL(20,6) DEFAULT NULL AFTER `unit_price_uah`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_withdrawals' AND COLUMN_NAME='shipment_id') > 0,
    'SELECT ''Column shipment_id already exists'' AS Info',
    'ALTER TABLE `warehouse_withdrawals` ADD COLUMN `shipment_id` BIGINT DEFAULT NULL AFTER `total_cost_uah`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add indexes for warehouse_withdrawals
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_withdrawals' 
     AND INDEX_NAME='idx_warehouse_withdrawals_warehouse_product') > 0,
    'SELECT ''Index idx_warehouse_withdrawals_warehouse_product already exists'' AS Info',
    'ALTER TABLE `warehouse_withdrawals` ADD KEY `idx_warehouse_withdrawals_warehouse_product` (`warehouse_id`, `product_id`)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_withdrawals' 
     AND INDEX_NAME='idx_warehouse_withdrawals_shipment') > 0,
    'SELECT ''Index idx_warehouse_withdrawals_shipment already exists'' AS Info',
    'ALTER TABLE `warehouse_withdrawals` ADD KEY `idx_warehouse_withdrawals_shipment` (`shipment_id`)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add foreign key for shipment_id (check if not exists first)
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_withdrawals' 
     AND CONSTRAINT_NAME='fk_warehouse_withdrawals_shipment') > 0,
    'SELECT ''Foreign key fk_warehouse_withdrawals_shipment already exists'' AS Info',
    'ALTER TABLE `warehouse_withdrawals` ADD CONSTRAINT `fk_warehouse_withdrawals_shipment` FOREIGN KEY (`shipment_id`) REFERENCES `shipments` (`id`) ON DELETE SET NULL'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =====================================================
-- STEP 3: RENAME TABLE (better semantics)
-- =====================================================

-- Rename warehouse_entries to warehouse_receipts
-- "Receipt" better represents incoming goods to warehouse
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='warehouse_receipts') > 0,
    'SELECT ''Table warehouse_receipts already exists'' AS Info',
    'RENAME TABLE `warehouse_entries` TO `warehouse_receipts`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Restore SQL notes setting
SET SQL_NOTES=@OLD_SQL_NOTES;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check new tables exist:
-- SELECT COUNT(*) FROM driver_product_balances;
-- SELECT COUNT(*) FROM warehouse_product_balances;
-- SELECT COUNT(*) FROM shipments;

-- Check renamed table:
-- SHOW TABLES LIKE 'warehouse_receipts';

-- Check new columns:
-- DESCRIBE purchases;
-- DESCRIBE warehouse_receipts;
-- DESCRIBE warehouse_withdrawals;

-- =====================================================
-- DONE! Your database is ready for the balance system
-- =====================================================

