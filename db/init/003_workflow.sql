SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'merchant_stores' AND column_name = 'review_remark') = 0,
    'ALTER TABLE merchant_stores ADD COLUMN review_remark VARCHAR(255) NULL AFTER address',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'merchant_stores' AND column_name = 'reviewed_at') = 0,
    'ALTER TABLE merchant_stores ADD COLUMN reviewed_at DATETIME NULL AFTER review_remark',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'rejected_reason') = 0,
    'ALTER TABLE orders ADD COLUMN rejected_reason VARCHAR(255) NULL AFTER verification_code',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'verified_at') = 0,
    'ALTER TABLE orders ADD COLUMN verified_at DATETIME NULL AFTER delivered_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'verified_by_store_id') = 0,
    'ALTER TABLE orders ADD COLUMN verified_by_store_id BIGINT UNSIGNED NULL AFTER verified_at',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'completed_at') = 0,
    'ALTER TABLE orders ADD COLUMN completed_at DATETIME NULL AFTER verified_by_store_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO accounts (username, password_hash, phone, role, status)
VALUES (
    'admin',
    '$2a$10$tNxozLajZ1BFLANvBdV4d.tv0ebhpDWP.NbCV9ci/tgGk2xOA2MES',
    '10000000000',
    'ADMIN',
    'ACTIVE'
);
