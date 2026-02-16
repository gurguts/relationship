
ALTER TABLE vehicles DROP COLUMN is_our_vehicle;
ALTER TABLE vehicles ADD COLUMN manager_id BIGINT NULL;
