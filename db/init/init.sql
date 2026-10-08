-- cat-maintain 数据库初始化脚本
-- 执行顺序：基础账号/门店 -> 业务表 -> 工作流字段与管理员 -> 完整性约束

CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_accounts_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS merchant_stores (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_merchant_stores_account_id (account_id),
    CONSTRAINT fk_merchant_stores_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sku VARCHAR(50) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    brand VARCHAR(50) NULL,
    description TEXT NULL,
    price DECIMAL(10, 2) NOT NULL,
    labor_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    stock INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    store_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    stock_deducted BOOLEAN NOT NULL DEFAULT FALSE,
    product_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    labor_fee_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    verification_code CHAR(8) NULL,
    approved_at DATETIME NULL,
    delivered_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_no (order_no),
    UNIQUE KEY uk_orders_verification_code (verification_code),
    KEY idx_orders_account_id (account_id),
    KEY idx_orders_store_id (store_id),
    CONSTRAINT fk_orders_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_orders_store_id
        FOREIGN KEY (store_id) REFERENCES merchant_stores (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    unit_labor_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order_id (order_id),
    KEY idx_order_items_product_id (product_id),
    CONSTRAINT fk_order_items_order_id
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product_id
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    store_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NULL,
    appointment_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    vehicle_plate VARCHAR(20) NOT NULL,
    vehicle_model VARCHAR(100) NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_appointments_account_id (account_id),
    KEY idx_appointments_store_time (store_id, appointment_time),
    CONSTRAINT fk_appointments_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_appointments_store_id
        FOREIGN KEY (store_id) REFERENCES merchant_stores (id),
    CONSTRAINT fk_appointments_order_id
        FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS maintenance_records (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    store_id BIGINT UNSIGNED NOT NULL,
    service_started_at DATETIME NOT NULL,
    service_completed_at DATETIME NULL,
    mileage INT UNSIGNED NULL,
    content TEXT NOT NULL,
    remark VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_maintenance_records_account_id (account_id),
    KEY idx_maintenance_records_store_id (store_id),
    CONSTRAINT fk_maintenance_records_appointment_id
        FOREIGN KEY (appointment_id) REFERENCES appointments (id),
    CONSTRAINT fk_maintenance_records_order_id
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_maintenance_records_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_maintenance_records_store_id
        FOREIGN KEY (store_id) REFERENCES merchant_stores (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    store_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    content VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_account_order (account_id, order_id),
    KEY idx_reviews_store_id (store_id),
    CONSTRAINT fk_reviews_account_id
        FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_reviews_store_id
        FOREIGN KEY (store_id) REFERENCES merchant_stores (id),
    CONSTRAINT fk_reviews_order_id
        FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- MySQL 8.4 不支持 ADD COLUMN IF NOT EXISTS，使用 information_schema 做幂等迁移。
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'merchant_stores' AND column_name = 'review_remark') = 0,
    'ALTER TABLE merchant_stores ADD COLUMN review_remark VARCHAR(255) NULL AFTER address',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 到店登记时间采用幂等迁移，已有数据卷也可以重新导入本脚本。
-- 旧版本下单即扣库存，已有订单标记为已扣减，防止升级后配送时再扣一次。
SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'stock_deducted') = 0,
    'ALTER TABLE orders ADD COLUMN stock_deducted BOOLEAN NOT NULL DEFAULT TRUE AFTER status',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
ALTER TABLE orders ALTER COLUMN stock_deducted SET DEFAULT FALSE;

-- 修复旧版本被拒绝订单未返还的库存；标记与库存同事务更新，重复导入不会重复返还。
START TRANSACTION;
UPDATE products p
JOIN (
    SELECT i.product_id, SUM(i.quantity) AS quantity
    FROM order_items i JOIN orders o ON o.id = i.order_id
    WHERE o.status = 'REJECTED' AND o.stock_deducted = TRUE
    GROUP BY i.product_id
) reserved ON reserved.product_id = p.id
SET p.stock = p.stock + reserved.quantity;
UPDATE orders SET stock_deducted = FALSE WHERE status = 'REJECTED' AND stock_deducted = TRUE;
COMMIT;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'appointments' AND column_name = 'checked_in_at') = 0,
    'ALTER TABLE appointments ADD COLUMN checked_in_at DATETIME NULL AFTER remark',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS product_reviews (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    content VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_reviews_account_order_product (account_id, order_id, product_id),
    KEY idx_product_reviews_product_created (product_id, created_at),
    CONSTRAINT fk_product_reviews_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT fk_product_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_product_reviews_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_product_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

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

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'maintenance_records'
       AND index_name = 'uk_maintenance_records_appointment') = 0,
    'ALTER TABLE maintenance_records ADD UNIQUE KEY uk_maintenance_records_appointment (appointment_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
