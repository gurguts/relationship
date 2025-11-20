-- Fix transaction type column size to support new transaction types
-- This script increases the size of the 'type' column to accommodate longer enum values
-- like EXTERNAL_INCOME, INTERNAL_TRANSFER, CURRENCY_CONVERSION, etc.

ALTER TABLE `transactions` 
    MODIFY COLUMN `type` VARCHAR(50) NOT NULL COMMENT 'Тип транзакции (расширен для новых типов)';

