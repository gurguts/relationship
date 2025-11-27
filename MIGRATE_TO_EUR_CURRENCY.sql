-- =====================================================
-- МИГРАЦИЯ: Переход на учет в EUR
-- =====================================================
-- Этот скрипт:
-- 1. Создает таблицу exchange_rates для хранения курсов валют
-- 2. Переименовывает поля в таблице purchases (UAH -> EUR)
-- 3. Добавляет поле quantity_eur для хранения количества в EUR
--
-- ВАЖНО: Перед выполнением убедитесь, что:
-- 1. Сделана резервная копия базы данных
-- 2. Приложение остановлено
-- =====================================================

USE export_data;

-- =====================================================
-- ШАГ 1: Создание таблицы exchange_rates
-- =====================================================
CREATE TABLE IF NOT EXISTS `exchange_rates` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `from_currency` VARCHAR(3) NOT NULL,
    `to_currency` VARCHAR(3) NOT NULL DEFAULT 'EUR',
    `rate` DECIMAL(20, 6) NOT NULL,
    `created_at` DATETIME(6) NOT NULL,
    `updated_at` DATETIME(6) NOT NULL,
    `updated_by_user_id` BIGINT,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_from_currency` (`from_currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- ШАГ 2: Переименование полей в таблице purchases
-- =====================================================
-- Переименовываем total_price_uah -> total_price_eur
ALTER TABLE `purchases`
CHANGE COLUMN `total_price_uah` `total_price_eur` DECIMAL(20, 6) NULL DEFAULT NULL;

-- Переименовываем unit_price_uah -> unit_price_eur
ALTER TABLE `purchases`
CHANGE COLUMN `unit_price_uah` `unit_price_eur` DECIMAL(20, 6) NULL DEFAULT NULL;

-- =====================================================
-- ШАГ 3: Добавление поля quantity_eur
-- =====================================================
ALTER TABLE `purchases`
ADD COLUMN `quantity_eur` DECIMAL(20, 2) NULL DEFAULT NULL AFTER `quantity`;

-- =====================================================
-- ШАГ 4: Заполнение quantity_eur для существующих записей
-- =====================================================
-- Для записей с валютой EUR или NULL: quantity_eur = quantity
-- Для записей с другой валютой: quantity_eur = quantity (пока не знаем курс, оставляем как есть)
-- ВАЖНО: После установки курсов валют нужно будет пересчитать quantity_eur для старых записей
UPDATE `purchases`
SET `quantity_eur` = `quantity`
WHERE `currency` IS NULL OR `currency` = 'EUR' OR `currency` = '';

-- Для записей с UAH или USD: пока оставляем quantity_eur = quantity
-- После установки курсов валют нужно будет пересчитать:
-- UPDATE purchases SET quantity_eur = quantity * (SELECT rate FROM exchange_rates WHERE from_currency = purchases.currency) WHERE currency IN ('UAH', 'USD');

-- =====================================================
-- ПРОВЕРКА: Убеждаемся, что все изменения применены
-- =====================================================
-- Проверяем наличие таблицы exchange_rates
SELECT COUNT(*) AS exchange_rates_table_exists
FROM information_schema.tables
WHERE table_schema = 'export_data' AND table_name = 'exchange_rates';

-- Проверяем наличие полей в purchases
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE
FROM information_schema.columns
WHERE table_schema = 'export_data' 
  AND table_name = 'purchases'
  AND COLUMN_NAME IN ('total_price_eur', 'unit_price_eur', 'quantity_eur');

-- =====================================================
-- ШАГ 5: Переименование полей в таблице driver_product_balances
-- =====================================================
  
-- =====================================================
-- ИНИЦИАЛИЗАЦИЯ: Создание начальных курсов валют (опционально)
-- =====================================================
-- Раскомментируйте и установите нужные курсы:
-- INSERT INTO `exchange_rates` (`from_currency`, `to_currency`, `rate`, `created_at`, `updated_at`)
-- VALUES 
--     ('UAH', 'EUR', 0.025000, NOW(), NOW()),
--     ('USD', 'EUR', 0.920000, NOW(), NOW())
-- ON DUPLICATE KEY UPDATE `rate` = VALUES(`rate`), `updated_at` = NOW();

